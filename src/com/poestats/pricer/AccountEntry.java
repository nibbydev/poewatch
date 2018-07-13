package com.poestats.pricer;

public class AccountEntry {
    public String account, character;
    public Integer league;

    public AccountEntry(String account, String character, Integer league) {
        this.account = account;
        this.character = character;
        this.league = league;
    }
}
