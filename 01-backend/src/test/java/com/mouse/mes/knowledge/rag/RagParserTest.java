package com.mouse.mes.knowledge.rag;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;

class RagParserTest {
    private final RagParser parser=new RagParser();
    @Test void pdfKeepsPages() throws Exception {
        byte[] bytes;
        try(var doc=new PDDocument();var out=new ByteArrayOutputStream()) {
            for(String text:List.of("Page one","Page two")) {
                var page=new PDPage();doc.addPage(page);
                try(var stream=new PDPageContentStream(doc,page)){stream.beginText();stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA),12);stream.newLineAtOffset(40,700);stream.showText(text);stream.endText();}
            }
            doc.save(out);bytes=out.toByteArray();
        }
        var parts=parser.parse("a.pdf",bytes);
        assertEquals(2,parts.size());assertEquals("第2页",parts.get(1).location());assertTrue(parts.get(1).text().contains("Page two"));
    }
    byte[] docx(String xml) throws Exception {
        var out=new ByteArrayOutputStream();try(var zip=new ZipOutputStream(out)){zip.putNextEntry(new ZipEntry("word/document.xml"));zip.write(xml.getBytes(StandardCharsets.UTF_8));zip.closeEntry();}return out.toByteArray();
    }
    @Test void docxReadsParagraphsAndRejectsExternalEntities() throws Exception {
        var parts=parser.parse("a.docx",docx("<w:document xmlns:w='http://schemas.openxmlformats.org/wordprocessingml/2006/main'><w:body><w:p><w:r><w:t>点胶操作</w:t></w:r></w:p></w:body></w:document>"));
        assertEquals("点胶操作",parts.get(0).text());assertEquals("段落1",parts.get(0).location());
        assertThrows(Exception.class,()->parser.parse("bad.docx",docx("<!DOCTYPE x [<!ENTITY ext SYSTEM 'file:///C:/Windows/win.ini'>]><x>&ext;</x>")));
    }
    @Test void chunkOverlapLimitsAndNoSilentTruncation() throws Exception {
        var parts=parser.parse("a.md","中".repeat(4000).getBytes(StandardCharsets.UTF_8));
        var chunks=parser.chunks("a",1,"标题","build",parts);
        assertEquals("build:0",chunks.get(0).id());assertEquals(1800,chunks.get(0).text().length());
        assertEquals(chunks.get(0).text().substring(1650),chunks.get(1).text().substring(0,150));
        assertThrows(IllegalArgumentException.class,()->parser.parse("a.txt","中".repeat(12001).getBytes(StandardCharsets.UTF_8)));
        assertThrows(IllegalArgumentException.class,()->parser.parse("a.txt",new byte[]{(byte)0xff}));
        assertThrows(IllegalArgumentException.class,()->parser.parse("a.doc",new byte[]{1,2}));
    }
}
