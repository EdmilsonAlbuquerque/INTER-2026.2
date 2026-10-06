package com.example.sistema.gerenciamento.api.Service;

import java.io.*;
import java.util.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;

@Service
public class RelatorioService {
    public byte[] gerar(String formato, String titulo, String contexto, List<String> colunas, List<List<String>> linhas) throws IOException {
        if ("xlsx".equals(formato)) return excel(titulo, contexto, colunas, linhas);
        if (!"pdf".equals(formato)) throw new IllegalArgumentException("Escolha PDF ou Excel (xlsx).");
        try (var document = new PDDocument(); var output = new ByteArrayOutputStream()) {
            var font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            var textos = new ArrayList<String>();
            textos.add(titulo); textos.add(contexto); textos.add("Total de registros: " + linhas.size()); textos.add("");
            int index = 0;
            for (var linha : linhas) {
                textos.add("Registro " + ++index);
                for (int c = 0; c < colunas.size(); c++) {
                    String text = colunas.get(c) + ": " + linha.get(c);
                    StringBuilder line = new StringBuilder();
                    for (char ch : text.replaceAll("[\\r\\n\\t]", " ").toCharArray()) {
                        String value = String.valueOf(ch);
                        try { font.encode(value); } catch (IllegalArgumentException exception) { value = "?"; }
                        if (font.getStringWidth(line + value) / 1000 * 10 > 505) {
                            textos.add(line.toString()); line.setLength(0);
                        }
                        line.append(value);
                    }
                    textos.add(line.toString());
                }
                textos.add("");
            }
            for (int start = 0; start < textos.size(); start += 46) {
                var page = new PDPage(); document.addPage(page);
                try (var stream = new PDPageContentStream(document, page)) {
                    stream.beginText(); stream.setFont(font, 10); stream.setLeading(15); stream.newLineAtOffset(40, 750);
                    for (int i = start; i < Math.min(start + 46, textos.size()); i++) { stream.showText(textos.get(i)); stream.newLine(); }
                    stream.endText(); stream.beginText(); stream.setFont(font, 9); stream.newLineAtOffset(40, 25);
                    stream.showText("Instituto Manancial - Página " + document.getNumberOfPages()); stream.endText();
                }
            }
            document.save(output); return output.toByteArray();
        }
    }
    private byte[] excel(String titulo, String contexto, List<String> colunas, List<List<String>> linhas) throws IOException {
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Relatorio");
            sheet.createRow(0).createCell(0).setCellValue(titulo);
            sheet.createRow(1).createCell(0).setCellValue(contexto);
            var style = workbook.createCellStyle(); var font = workbook.createFont(); font.setBold(true); style.setFont(font);
            var header = sheet.createRow(3);
            for (int i=0;i<colunas.size();i++) { var cell=header.createCell(i);cell.setCellValue(colunas.get(i));cell.setCellStyle(style); }
            int row = 4;
            for (var linha : linhas) {
                var current = sheet.createRow(row++);
                for (int i=0;i<linha.size();i++) current.createCell(i, CellType.STRING).setCellValue(linha.get(i));
            }
            for(int i=0;i<colunas.size();i++) sheet.setColumnWidth(i, i==1 ? 10000 : 6500);
            sheet.createFreezePane(0,4);
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(3, Math.max(3,row-1),0,colunas.size()-1));
            workbook.write(output); return output.toByteArray();
        }
    }
}
