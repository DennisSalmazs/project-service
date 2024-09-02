package com.teambridge.service.impl;

import com.teambridge.dto.ProjectDTO;
import com.teambridge.entity.Project;
import com.teambridge.enums.Status;
import com.teambridge.exception.ProjectAccessDeniedException;
import com.teambridge.exception.ProjectAlreadyExistsException;
import com.teambridge.exception.ProjectIsCompletedException;
import com.teambridge.exception.ProjectNotFoundException;
import com.teambridge.repository.ProjectRepository;
import com.teambridge.service.KeycloakService;
import com.teambridge.service.ProjectService;
import com.teambridge.util.MapperUtil;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final MapperUtil mapperUtil;
    private final KeycloakService keycloakService;

    public ProjectServiceImpl(ProjectRepository projectRepository, MapperUtil mapperUtil, KeycloakService keycloakService) {
        this.projectRepository = projectRepository;
        this.mapperUtil = mapperUtil;
        this.keycloakService = keycloakService;
    }

    @Override
    public ProjectDTO create(ProjectDTO projectDTO) {

        Optional<Project> foundProject = projectRepository.findByProjectCode(projectDTO.getProjectCode());

        if (foundProject.isPresent()) {
            throw new ProjectAlreadyExistsException("Project already exists.");
        }

        Project projectToSave = mapperUtil.convert(projectDTO, new Project());

        String loggedInUserUsername = keycloakService.getUsername();

        projectToSave.setAssignedManager(loggedInUserUsername);
        projectToSave.setProjectStatus(Status.OPEN);

        Project savedProject = projectRepository.save(projectToSave);

        return mapperUtil.convert(savedProject, new ProjectDTO());

    }

    @Override
    public ProjectDTO readByProjectCode(String projectCode) {

        Project foundProject = projectRepository.findByProjectCode(projectCode)
                .orElseThrow(() -> new ProjectNotFoundException("Project does not exist."));

        checkAccess(foundProject);

        return mapperUtil.convert(foundProject, new ProjectDTO());

    }

    @Override
    public String readManagerByProjectCode(String projectCode) {

        Project foundProject = projectRepository.findByProjectCode(projectCode)
                .orElseThrow(() -> new ProjectNotFoundException("Project does not exist."));

        checkAccess(foundProject);

        return foundProject.getAssignedManager();

    }

    @Override
    public List<ProjectDTO> readAllProjectsWithDetails() {

        String loggedInUserUsername = keycloakService.getUsername();

        List<Project> foundProjects = projectRepository.findAllByAssignedManager(loggedInUserUsername);
        return foundProjects.stream()
                .map(this::retrieveProjectDetails).collect(Collectors.toList());

    }

    @Override
    public List<ProjectDTO> adminReadAllProjects() {
        List<Project> foundProjects = projectRepository.findAll();
        return foundProjects.stream()
                .map(project -> mapperUtil.convert(project, new ProjectDTO())).collect(Collectors.toList());
    }

    @Override
    public List<ProjectDTO> managerReadAllProjects() {

        String loggedInUserUsername = keycloakService.getUsername();

        List<Project> foundProjects = projectRepository.findAllByAssignedManager(loggedInUserUsername);

        return foundProjects.stream()
                .map(project -> mapperUtil.convert(project, new ProjectDTO())).collect(Collectors.toList());

    }

    @Override
    public Integer countNonCompletedByAssignedManager(String assignedManager) {
        return projectRepository.countNonCompletedByAssignedManager(assignedManager);
    }

    @Override
    public boolean checkByProjectCode(String projectCode) {

        Optional<Project> foundProject = projectRepository.findByProjectCode(projectCode);

        if (foundProject.isEmpty()) {
            return false;
        }

        if (foundProject.get().getProjectStatus().getValue().equals("Completed")) {
            throw new ProjectIsCompletedException("Project is already completed.");
        }

        checkAccess(foundProject.get());

        return true;

    }

    @Override
    public ProjectDTO update(String projectCode, ProjectDTO projectDTO) {

        Project foundProject = projectRepository.findByProjectCode(projectCode)
                .orElseThrow(() -> new ProjectNotFoundException("Project does not exist."));

        checkAccess(foundProject);

        Project projectToUpdate = mapperUtil.convert(projectDTO, new Project());
        projectToUpdate.setId(foundProject.getId());
        projectToUpdate.setProjectCode(projectCode);
        projectToUpdate.setProjectStatus(foundProject.getProjectStatus());
        projectToUpdate.setAssignedManager(foundProject.getAssignedManager());

        Project updatedProject = projectRepository.save(projectToUpdate);

        return mapperUtil.convert(updatedProject, new ProjectDTO());

    }

    @Override
    public ProjectDTO complete(String projectCode) {

        Project projectToComplete = projectRepository.findByProjectCode(projectCode)
                .orElseThrow(() -> new ProjectNotFoundException("Project does not exist."));

        checkAccess(projectToComplete);

        projectToComplete.setProjectStatus(Status.COMPLETED);

        Project completedProject = projectRepository.save(projectToComplete);

        completeRelatedTasks(projectCode);

        return mapperUtil.convert(completedProject, new ProjectDTO());

    }

    @Override
    public void delete(String projectCode) {

        Project projectToDelete = projectRepository.findByProjectCode(projectCode)
                .orElseThrow(() -> new ProjectNotFoundException("Project does not exist."));

        checkAccess(projectToDelete);

        projectToDelete.setIsDeleted(true);
        projectToDelete.setProjectCode(projectCode + "-" + projectToDelete.getId());

        deleteRelatedTasks(projectCode);

        projectRepository.save(projectToDelete);

    }

    private void checkAccess(Project project) {

        String loggedInUserUsername = keycloakService.getUsername();

        if ((keycloakService.hasClientRole(loggedInUserUsername, "Manager") && !loggedInUserUsername.equals(project.getAssignedManager()))
                || keycloakService.hasClientRole(loggedInUserUsername, "Employee")) {
            throw new ProjectAccessDeniedException("Access denied, make sure that you are working on your own project.");
        }

    }

    private ProjectDTO retrieveProjectDetails(Project project) {

        //TODO Retrieve the completed and non-completed task counts from task-service

        return new ProjectDTO();

    }

    private void completeRelatedTasks(String projectCode) {

        //TODO Send a request to task-service to complete all the tasks of a certain project

    }

    private void deleteRelatedTasks(String projectCode) {

        //TODO Send a request to task-service to delete all the tasks of a certain project

    }

}
