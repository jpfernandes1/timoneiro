package com.jompastech.backend.integration.controller.util;

public class CpfGenerator {

    public static String generateValidCpf() {
        int[] cpf = new int[11];
        for (int i = 0; i < 9; i++) cpf[i] = (int) (Math.random() * 10);
        cpf[9] = calculateCpfDigit(cpf, 9);
        cpf[10] = calculateCpfDigit(cpf, 10);
        StringBuilder sb = new StringBuilder();
        for (int d : cpf) sb.append(d);
        return sb.toString();
    }

    private static int calculateCpfDigit(int[] cpf, int length) {
        int sum = 0, weight = length + 1;
        for (int i = 0; i < length; i++) sum += cpf[i] * weight--;
        int mod = (sum * 10) % 11;
        return mod == 10 ? 0 : mod;
    }
}
