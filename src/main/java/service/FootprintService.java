package service;

import dao.*;
import model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * FootprintService - Logika bisnis perhitungan Blue Water Footprint dan distribusi credit.
 * Migrasi dari Spring @Service FootprintService.
 */
public class FootprintService {

    private final CompanyDAO        companyDAO    = new CompanyDAO();
    private final IndividualDAO     individualDAO = new IndividualDAO();
    private final GovernmentDAO     governmentDAO = new GovernmentDAO();
    private final WaterFootprintDAO wfDAO         = new WaterFootprintDAO();

    /**
     * Hitung Blue Water Footprint (BWF) untuk Government.
     * Formula: BWF = blueWaterIncorporation + lostReturnFlow * (volume / time)
     */
    public double calculateGovernmentFootprint(double blueWaterIncorporation,
                                               double lostReturnFlow,
                                               double volume,
                                               double time,
                                               User user) {
        Government gov = governmentDAO.findByUser(user)
            .orElseThrow(() -> new RuntimeException("Profil Government tidak ditemukan."));

        double result = blueWaterIncorporation + lostReturnFlow * (volume / time);

        WaterFootprint wf = gov.getWaterFootprint();
        if (wf == null) wf = new WaterFootprint();
        wf.setTotalUsage(0);
        wf.setCategory("General");
        wf.setCalculateFootprint((float) result);
        wf = wfDAO.save(wf);

        gov.setWaterFootprint(wf);
        governmentDAO.update(gov);

        return result;
    }

    /**
     * Dapatkan BWF regional (dari Government di region yang sama dengan Company/Individual yang login).
     */
    public double getRegionalFootprint(User user) {
        Company company = companyDAO.findByUser(user)
            .orElseThrow(() -> new RuntimeException("Profil Company tidak ditemukan."));
        if (company.getRegion() == null)
            throw new RuntimeException("Region perusahaan belum diset.");

        Government gov = governmentDAO.findFirstByRegion(company.getRegion())
            .orElseThrow(() -> new RuntimeException("Belum ada Government di region Anda."));

        if (gov.getWaterFootprint() == null)
            throw new RuntimeException("Government di region Anda belum menghitung BWF.");

        return gov.getWaterFootprint().getCalculateFootprint();
    }

    /**
     * Hitung water credit perusahaan.
     * Formula: credit = (BWF * eta) * (1 - sl) / nc
     */
    public double calculateCompanyCredit(double eta, double sl, User user) {
        double bwfCalc = getRegionalFootprint(user);
        // nc = jumlah perusahaan approved di region ini
        Company company = companyDAO.findByUser(user)
            .orElseThrow(() -> new RuntimeException("Profil Company tidak ditemukan."));

        List<Company> allInRegion = companyDAO.findByRegion(company.getRegion());
        long nc = allInRegion.stream()
            .filter(c -> c.getUser() != null && c.getUser().isApproved())
            .count();
        if (nc == 0) nc = 1;

        double result = (bwfCalc * eta) * (1 - sl) / nc;
        company.setWatercredit(result);
        companyDAO.update(company);
        return result;
    }

    /**
     * Distribusikan BWF ke semua Company dan Individual di region Government yang login.
     * @return Map dengan hasil distribusi
     */
    public Map<String, Object> distributeCredit(double blueWaterIncorporation,
                                                double lostReturnFlow,
                                                double volume,
                                                double time,
                                                User user) {
        Government gov = governmentDAO.findByUser(user)
            .orElseThrow(() -> new RuntimeException("Profil Government tidak ditemukan."));
        if (gov.getRegion() == null)
            throw new RuntimeException("Region Government belum diset.");

        // 1. Hitung dan simpan BWF
        double bwf = blueWaterIncorporation + lostReturnFlow * (volume / time);
        WaterFootprint wf = gov.getWaterFootprint();
        if (wf == null) wf = new WaterFootprint();
        wf.setTotalUsage(0);
        wf.setCategory("General");
        wf.setCalculateFootprint((float) bwf);
        wf = wfDAO.save(wf);
        gov.setWaterFootprint(wf);
        governmentDAO.update(gov);

        // 2. Distribusi ke Company approved di region ini
        List<Company> companies = companyDAO.findByRegion(gov.getRegion()).stream()
            .filter(c -> c.getUser() != null && c.getUser().isApproved())
            .toList();

        if (companies.isEmpty())
            throw new RuntimeException("Belum ada perusahaan yang disetujui di region Anda.");

        int nc = companies.size();
        double totalCompanyCredit = 0.0;
        List<Map<String, Object>> companyResults = new ArrayList<>();

        for (Company company : companies) {
            if (company.getEta() == null || company.getSl() == null) {
                throw new RuntimeException(
                    "Gagal: Perusahaan '" + (company.getUser() != null ? company.getUser().getName() : "?")
                    + "' belum mengisi parameter Efisiensi (η) dan Kelangkaan Air (Sl).");
            }
            double credit = (bwf * company.getEta()) * (1 - company.getSl()) / nc;
            company.setWatercredit(credit);
            companyDAO.update(company);
            totalCompanyCredit += credit;

            Map<String, Object> cMap = new HashMap<>();
            cMap.put("companyName", company.getUser() != null ? company.getUser().getName() : "?");
            cMap.put("eta", company.getEta());
            cMap.put("sl", company.getSl());
            cMap.put("credit", credit);
            companyResults.add(cMap);
        }

        double averageCompanyCredit = totalCompanyCredit / nc;

        // 3. Distribusi ke Individual approved di region ini
        List<Individual> individuals = individualDAO.findByRegion(gov.getRegion()).stream()
            .filter(i -> i.getUser() != null && i.getUser().isApproved())
            .toList();

        List<Map<String, Object>> individualResults = new ArrayList<>();
        for (Individual individual : individuals) {
            double individualCredit = (averageCompanyCredit > 0) ? (bwf / averageCompanyCredit) * 4.0 : 0;
            individual.setWaterCredit(individualCredit);
            individualDAO.update(individual);

            Map<String, Object> iMap = new HashMap<>();
            iMap.put("name", individual.getUser() != null ? individual.getUser().getName() : "?");
            iMap.put("credit", individualCredit);
            individualResults.add(iMap);
        }

        // 4. Build response map
        Map<String, Object> response = new HashMap<>();
        response.put("bwf", bwf);
        response.put("averageCompanyCredit", averageCompanyCredit);
        response.put("companies", companyResults);
        response.put("individuals", individualResults);
        response.put("regionName", gov.getRegion().getName());
        return response;
    }

    /**
     * Total water credit semua Company di region user (Individual).
     */
    public double getRegionalCompanyCredit(User user) {
        Individual individual = individualDAO.findByUser(user)
            .orElseThrow(() -> new RuntimeException("Profil Individual tidak ditemukan."));
        if (individual.getRegion() == null)
            throw new RuntimeException("Region Individual belum diset.");

        double totalCredit = companyDAO.findByRegion(individual.getRegion()).stream()
            .filter(c -> c.getWatercredit() != null)
            .mapToDouble(Company::getWatercredit)
            .sum();

        if (totalCredit == 0)
            throw new RuntimeException("Belum ada perusahaan di region Anda yang memiliki Water Credit.");
        return totalCredit;
    }
}
