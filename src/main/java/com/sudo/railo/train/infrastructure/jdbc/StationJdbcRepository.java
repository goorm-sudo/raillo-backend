package com.sudo.railo.train.infrastructure.jdbc;

import java.util.List;

import com.sudo.railo.train.domain.Station;

public interface StationJdbcRepository {

	void bulkInsertStations(List<Station> stations);
}
