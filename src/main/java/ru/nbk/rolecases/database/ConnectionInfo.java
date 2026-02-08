package ru.nbk.rolecases.database;

public record ConnectionInfo(String host, int port, String database, String user, String password, int groupConcatLength) {

}
