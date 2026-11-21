package com.bnpp.regliss.entity;

import java.util.Locale;

public enum Language {

    EN, FR;

    public Locale toLocale() { return new Locale(name()); }
}
