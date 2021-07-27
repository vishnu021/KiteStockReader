package com.vish.fando.stock.live;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.vish.fando.stock.model.Transaction;
import com.vish.fando.stock.dao.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class TransactionService {
	@Autowired
	TransactionRepository transactionRepo;

	ArrayList<Transaction> allTransactions = new ArrayList<>();
	int bufferLength;

	public void publish(List<Transaction> transactions) {
		try {

			this.allTransactions.addAll(transactions);

			if (bufferLength++ > 10) {
				bufferLength = 0;
				writeTransactionsToDB(allTransactions);
				allTransactions = new ArrayList<Transaction>();
			}
		} catch (Exception e) {
			log.error(e);
		}
	}

	public List<Transaction> getTransactionsForSymbol(String symbol, String date) {
		return transactionRepo.findBySymbolAndDate(symbol, date);
	}

	private void writeTransactionsToDB(List<Transaction> transactions) {
		try {
			transactionRepo.saveAll(transactions);
		} catch (Exception e) {
			log.error(transactions.stream().map(t -> t.getDateTime()).collect(Collectors.toList()));
			log.error(e);
		}
	}
}
