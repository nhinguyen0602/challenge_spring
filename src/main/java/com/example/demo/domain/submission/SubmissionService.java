package com.example.demo.domain.submission;

import com.example.demo.domain.challenge.Challenge;
import com.example.demo.domain.challenge.ChallengeService;
import com.example.demo.domain.employee.Employee;
import com.example.demo.domain.employee.EmployeeService;
import com.example.demo.domain.file.exception.BadRequestException;
import com.example.demo.integration.database.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionService {

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private ChallengeService challengeService;

    @Autowired
    private EmployeeService employeeService;

    public List<Submission> getSubmissionsByChallengeAndEmployee(String currentEmployeeEmail){
        Challenge currentChallenge = challengeService.getCurrentChallenge();
        Employee currentEmployee = employeeService.getCurrentEmployee(currentEmployeeEmail);
        return submissionRepository.findByChallengeAndEmployee(currentChallenge, currentEmployee);
    }

    public List<Submission> getSubmissionByDate(final String time){
        long duration = Long.parseLong(time);
        LocalDate date = Instant.ofEpochSecond(duration).atZone(ZoneId.systemDefault()).toLocalDate();
        return submissionRepository.findByDateCreated(date);
    }

    public Submission addSubmission(final String currentEmployeeEmail, Submission submission){
        Challenge currentChallenge = challengeService.getCurrentChallenge();
        Employee currentEmployee = employeeService.getCurrentEmployee(currentEmployeeEmail);
        Optional<Submission> submissionFilter = submissionRepository.findByChallengeAndEmployeeAndDateCreated(currentChallenge, currentEmployee, LocalDate.now());
        if(submissionFilter.isPresent()){
            throw new BadRequestException("existed");
        }
        if(getSubmissionsByChallengeAndEmployee(currentEmployeeEmail).size() == 0 ){
            currentChallenge.getEmployees().add(currentEmployee);
            challengeService.saveChallenge(currentChallenge);
        }
        submission.setChallenge(currentChallenge);
        submission.setEmployee(currentEmployee);
        submission.setDateCreated(LocalDate.now());
        return submissionRepository.save(submission);
    }

    public Submission getLastSubmission(final String currentEmployeeEmail){
        List<Submission> submissions = getSubmissionsByChallengeAndEmployee(currentEmployeeEmail);
        Submission submission = submissions.stream().max(Comparator.comparing(Submission::getDateCreated)).orElseThrow(NoSuchElementException::new);
        return submission;
    }

    public List<Submission> getAll(){
        Challenge currentChallenge = challengeService.getCurrentChallenge();
        List <Submission> submissionsResult = new ArrayList<>();
        for(Employee employee:currentChallenge.getEmployees()){
            submissionsResult.add(getBestSubmissionOfUser(employee.getEmail()));
        }
        return submissionsResult;
    }

    public Submission getBestSubmissionOfUser(String emailEmployee){
        List<Submission> submissions = getSubmissionsByChallengeAndEmployee(emailEmployee);
        Submission submission = submissions.stream().max(Comparator.comparing(Submission::getDuration)).orElseThrow(NoSuchElementException::new);
        return submission;
    }

}
