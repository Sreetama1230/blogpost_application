package com.example.demo.dto;

public class ReactDTO {

	
	private Long bpId;
	private boolean reaction;
	
	private Long uId;

	public Long getBpId() {
		return bpId;
	}

	public void setBpId(Long bpId) {
		this.bpId = bpId;
	}

	public boolean isReaction() {
		return reaction;
	}

	public void setReaction(boolean reaction) {
		this.reaction = reaction;
	}

	public Long getuId() {
		return uId;
	}

	public void setuId(Long uId) {
		this.uId = uId;
	}

	public ReactDTO(Long bpId, boolean reaction, Long uId) {
		super();
		this.bpId = bpId;
		this.reaction = reaction;
		this.uId = uId;
	}
	
	
	

	
}
