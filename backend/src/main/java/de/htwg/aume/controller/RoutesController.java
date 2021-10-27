package de.htwg.aume.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import de.htwg.aume.model.Member;
import de.htwg.aume.model.Session;
import de.htwg.aume.service.DatabaseService;
import lombok.val;

@CrossOrigin(origins = "http://localhost:8080/")
@RestController
public class RoutesController {

	@Autowired
	DatabaseService databaseService;

	@PostMapping(value = "/sessions")
	public ResponseEntity<Session> createSession() {
		val usedUuids = databaseService.getSessions().stream().map(s -> s.getSessionID()).collect(Collectors.toSet());
		val sessionUuids = Stream.generate(UUID::randomUUID).filter(s -> !usedUuids.contains(s)).limit(3)
				.collect(Collectors.toList());
		val session = new Session(sessionUuids.get(0), sessionUuids.get(1), sessionUuids.get(2), new ArrayList<Member>());
		databaseService.saveSession(session);
		return new ResponseEntity<Session>(session, HttpStatus.CREATED);
	}

	@PostMapping(value = "/sessions/{sessionID}/join")
	public ResponseEntity<String> joinSession(@PathVariable UUID sessionID, @RequestBody Member member) {
		if (!addMemberToSession(sessionID, member)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.sessionNotFoundErrorMessage);
		}
		return new ResponseEntity<>("", HttpStatus.OK);
	}

	@GetMapping(value = "/sessions/{sessionID}")
	public @ResponseBody Session getSession(@PathVariable UUID sessionID) {
		return ControllerUtils.getSessionOrThrowResponse(databaseService, sessionID);
	}

	private synchronized boolean addMemberToSession(UUID sessionID, Member member) {
		val session = databaseService.getSessionByID(sessionID).orElse(null);
		if (session == null) {
			return false;
		}
		List<Member> members = session.getMembers().stream().map(m -> m).collect(Collectors.toList());
		if (members.stream().anyMatch(m -> m.getMemberID().equals(member.getMemberID()))) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorMessages.memberExistsErrorMessage);
		}
		members.add(member);
		databaseService.saveSession(session.copyWith(null, null, null, members));
		return true;
	}
}