package com.vish.fando.stock.model;

import java.io.Serializable;

import javax.persistence.Embeddable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class SymbolDateModel implements Serializable {
	private static final long serialVersionUID = 1L;
	private String symbol;
	private String dateTime;
}
