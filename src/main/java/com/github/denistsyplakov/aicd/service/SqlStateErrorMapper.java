package com.github.denistsyplakov.aicd.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;

final class SqlStateErrorMapper {

    private SqlStateErrorMapper() {
    }

    static void throwForDataIntegrity(DataIntegrityViolationException exception, String fallbackMessage) {
        String sqlState = findSqlState(exception);
        if ("23505".equals(sqlState)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Resource with this key already exists", exception);
        }
        if ("23503".equals(sqlState)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Resource is referenced or references missing entity", exception);
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, fallbackMessage, exception);
    }

    private static String findSqlState(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException sqlException && sqlException.getSQLState() != null) {
                return sqlException.getSQLState();
            }
            current = current.getCause();
        }
        return null;
    }
}
