package app.service;



import app.model.entity.Fine;
import app.repository.FineRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class FineService {

    private final FineRepository fineRepo;

    @Autowired
    public FineService(FineRepository fineRepo) {
        this.fineRepo = fineRepo;
    }

    public String payFine(UUID fineId) {

        Fine fine = fineRepo.findById(fineId)
                .orElseThrow(() -> new RuntimeException("Fine not found"));

        fine.setPaid(true);
        fineRepo.save(fine);

        return "Fine paid successfully";
    }
}