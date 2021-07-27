package com.vish.fando.stock.model;

import lombok.NonNull;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
@Getter
@ToString
public class ScriptModel {
	@NonNull
	String indexName;
	@NonNull
	Integer tick;
	@NonNull
	String prefix;
	Integer lotSize;
}
