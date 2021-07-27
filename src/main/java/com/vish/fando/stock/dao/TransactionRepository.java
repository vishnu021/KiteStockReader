package com.vish.fando.stock.dao;

import java.util.List;

import com.vish.fando.stock.model.SymbolDateModel;
import com.vish.fando.stock.model.Transaction;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface TransactionRepository extends CrudRepository<Transaction, SymbolDateModel> {

	public List<Transaction> findAllByLtp(Double ltp);

	@Query(value = "select * from stocks.transaction where symbol = ?1 and date_time like ?2%", nativeQuery = true)
	public List<Transaction> findBySymbolAndDate(String symbol, String dateTime);

	@Query(value = "select * from stocks.transaction where date_time like ?1%", nativeQuery = true)
	public List<Transaction> findByDate(String dateTime);

	@Query(value = "select distinct(symbol) from stocks.transaction where date_time like ?1%", nativeQuery = true)
	public List<String> findSymbolsByDate(String dateTime);
}
