package com.mouse.mes.knowledge.rag;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.util.*;
import java.util.zip.*;

/** Extraction is bounded and never silently truncates the task-three text contract. */
@Component
public class RagParser {
    public static final int MAX_TEXT = 12000;
    public static final int MAX_BYTES = 2 * 1024 * 1024;
    public List<RagTypes.Part> parse(String filename, byte[] data) throws Exception {
        if (data.length == 0 || data.length > MAX_BYTES) throw new IllegalArgumentException("文件须为1字节至2MB");
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        List<RagTypes.Part> parts = new ArrayList<>();
        switch (ext) {
            case "txt", "md", "csv" -> {
                String text;
                try { text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(data)).toString(); }
                catch (CharacterCodingException e) { throw new IllegalArgumentException("文本必须为UTF-8编码，请另存为UTF-8后上传"); }
                parts.add(new RagTypes.Part(text.replace("\uFEFF", ""), "正文"));
            }
            case "pdf" -> {
                try (var doc = Loader.loadPDF(data)) {
                    if (doc.getNumberOfPages() > 100) throw new IllegalArgumentException("PDF最多100页");
                    PDFTextStripper stripper = new PDFTextStripper();
                    for (int page = 1; page <= doc.getNumberOfPages(); page++) {
                        stripper.setStartPage(page); stripper.setEndPage(page);
                        parts.add(new RagTypes.Part(stripper.getText(doc), "第" + page + "页"));
                        validateLength(parts);
                    }
                }
            }
            case "docx" -> {
                boolean found = false;
                try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(data))) {
                    ZipEntry entry;
                    while ((entry = zip.getNextEntry()) != null) {
                        if (!entry.getName().equals("word/document.xml")) continue;
                        byte[] xml = zip.readNBytes(4 * 1024 * 1024 + 1);
                        if (xml.length > 4 * 1024 * 1024) throw new IllegalArgumentException("DOCX正文解压后过大");
                        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                        factory.setNamespaceAware(true);
                        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                        factory.setXIncludeAware(false); factory.setExpandEntityReferences(false);
                        var dom = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
                        var paragraphs = dom.getElementsByTagNameNS("*", "p");
                        for (int i = 0; i < paragraphs.getLength(); i++) {
                            var runs = ((org.w3c.dom.Element) paragraphs.item(i)).getElementsByTagNameNS("*", "t");
                            StringBuilder text = new StringBuilder();
                            for (int j = 0; j < runs.getLength(); j++) text.append(runs.item(j).getTextContent());
                            if (!text.toString().isBlank()) parts.add(new RagTypes.Part(text.toString(), "段落" + (i + 1)));
                        }
                        found = true; break;
                    }
                }
                if (!found) throw new IllegalArgumentException("DOCX缺少正文，或文件格式不正确");
            }
            default -> throw new IllegalArgumentException("支持PDF、DOCX、TXT、MD、CSV，不支持旧版DOC及图片OCR");
        }
        validateLength(parts);
        if (parts.stream().allMatch(p -> p.text().isBlank())) throw new IllegalArgumentException("未提取到正文；扫描版PDF请先做OCR");
        return parts;
    }
    private void validateLength(List<RagTypes.Part> parts) {
        if (parts.stream().mapToInt(p -> p.text().length()).sum() + Math.max(0, parts.size()-1) > MAX_TEXT)
            throw new IllegalArgumentException("正文超过12000字符的双方约定，请拆分文件；系统不会截断正文");
    }
    public List<RagTypes.Chunk> chunks(String id, int version, String title, String buildId, List<RagTypes.Part> parts) {
        StringBuilder body = new StringBuilder();
        List<Integer> starts = new ArrayList<>();
        for (var part : parts) { if (!starts.isEmpty()) body.append('\n'); starts.add(body.length()); body.append(part.text()); }
        List<RagTypes.Chunk> result = new ArrayList<>();
        for (int start = 0; start < body.length();) {
            int end = Math.min(start + 1800, body.length());
            if(end<body.length()&&Character.isHighSurrogate(body.charAt(end-1)))end--;
            List<String> locations = new ArrayList<>();
            for (int i = 0; i < parts.size(); i++) {
                int stop = i + 1 < parts.size() ? starts.get(i + 1) : body.length();
                if (starts.get(i) < end && stop > start) locations.add(parts.get(i).location());
            }
            result.add(new RagTypes.Chunk(buildId + ":" + result.size(), id, version, title,
                    body.substring(start, end), String.join("、", locations)));
            if (end == body.length()) break;
            start=end-150;if(Character.isLowSurrogate(body.charAt(start)))start--;
        }
        return result;
    }
}
