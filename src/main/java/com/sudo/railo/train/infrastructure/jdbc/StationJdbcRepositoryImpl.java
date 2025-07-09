package com.sudo.railo.train.infrastructure.jdbc;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.sudo.railo.train.domain.Station;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class StationJdbcRepositoryImpl implements StationJdbcRepository {

	private final JdbcTemplate jdbcTemplate;

	@Override
	public void bulkInsertStations(List<Station> stations) {
		String sql = "INSERT INTO station (station_name) VALUES (?)";

		jdbcTemplate.batchUpdate(sql, stations, stations.size(), (ps, station) ->
			ps.setString(1, station.getStationName()));
	}
}
