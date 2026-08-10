package com.ai.contentmoderation.app.dto;

import java.util.List;

public class GeminiRequest {

	List<Content> contents;

	public List<Content> getContents() {
		return contents;
	}

	public void setContents(List<Content> contents) {
		this.contents = contents;
	}

	public GeminiRequest(List<Content> contents) {
		super();
		this.contents = contents;
	}

	public GeminiRequest() {
		super();
		// TODO Auto-generated constructor stub
	}

	
	
}


/*
 * 
 * {
    "contents": [
      {
        "parts": [
          {
            "text": "Explain how AI works in a few words"
          }
        ]
      }
    ]
  }
 * 
 * */
 
