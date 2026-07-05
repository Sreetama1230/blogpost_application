package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.FeedItem;
import com.example.demo.service.FeedService;

@RestController
@RequestMapping("/timeline")
public class FeedController {

	@Autowired
	FeedService feedService;

	@GetMapping
	public ResponseEntity<List<FeedItem>> timeline(int start, int size) {

		return new ResponseEntity<>(feedService.timeline(start, size), HttpStatus.OK);
	}
}
