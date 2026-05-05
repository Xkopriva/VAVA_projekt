package com.example.bais.services;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Vyvezenie a dovezení dat do XML
 */
public class DataXmlExporter {

    /**
     * Exportuje data do XML súboru
     */
    public static void exportToXml(String filePath, String userName, List<String> enrolledSubjects,
                                   List<String> grades, List<String> reminders) {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            Element rootElement = doc.createElement("bais_export");
            rootElement.setAttribute("exported_at", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
            rootElement.setAttribute("version", "1.0");
            doc.appendChild(rootElement);

            // Informace o uživateli
            Element userElement = doc.createElement("user");
            userElement.setAttribute("name", userName);
            rootElement.appendChild(userElement);

            // Předměty
            Element subjectsElement = doc.createElement("enrolled_subjects");
            for (String subject : enrolledSubjects) {
                Element subjectElement = doc.createElement("subject");
                subjectElement.setTextContent(subject);
                subjectsElement.appendChild(subjectElement);
            }
            rootElement.appendChild(subjectsElement);

            // Známky
            Element gradesElement = doc.createElement("grades");
            for (String grade : grades) {
                Element gradeElement = doc.createElement("grade");
                gradeElement.setTextContent(grade);
                gradesElement.appendChild(gradeElement);
            }
            rootElement.appendChild(gradesElement);

            // Připomínky
            Element remindersElement = doc.createElement("reminders");
            for (String reminder : reminders) {
                Element reminderElement = doc.createElement("reminder");
                reminderElement.setTextContent(reminder);
                remindersElement.appendChild(reminderElement);
            }
            rootElement.appendChild(remindersElement);

            // Uložení do souboru
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty("indent", "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(filePath));
            transformer.transform(source, result);

            System.out.println("✓ Data vyexportována do: " + filePath);
        } catch (Exception e) {
            System.err.println("✗ Chyba při exportu: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Import len pripomienok z xml suboru 
     */

    
    public static List<String> importRemindersOnly(String filePath) {
        List<String> reminders = new ArrayList<>();
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(new File(filePath));

            NodeList reminderNodes = doc.getElementsByTagName("reminder");
            for (int i = 0; i < reminderNodes.getLength(); i++) {
                reminders.add(reminderNodes.item(i).getTextContent());
            }
        } catch (Exception e) {
            System.err.println("✗ Chyba pri importe pripomienok: " + e.getMessage());
        }
        return reminders;
    }

    /**
     * Importuje data z XML súboru
     * @return Mapa s klíči: "user", "subjects", "grades", "reminders"
     */
    public static Map<String, Object> importFromXml(String filePath) {
        Map<String, Object> data = new HashMap<>();

        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(new File(filePath));

            Element rootElement = doc.getDocumentElement();

            // Uživatel
            NodeList userNodes = rootElement.getElementsByTagName("user");
            if (userNodes.getLength() > 0) {
                Element userElement = (Element) userNodes.item(0);
                data.put("user", userElement.getAttribute("name"));
            }

            // Předměty
            List<String> subjects = new ArrayList<>();
            NodeList subjectNodes = rootElement.getElementsByTagName("subject");
            for (int i = 0; i < subjectNodes.getLength(); i++) {
                Element el = (Element) subjectNodes.item(i);
                subjects.add(el.getTextContent());
            }
            data.put("subjects", subjects);

            // Známky
            List<String> grades = new ArrayList<>();
            NodeList gradeNodes = rootElement.getElementsByTagName("grade");
            for (int i = 0; i < gradeNodes.getLength(); i++) {
                Element el = (Element) gradeNodes.item(i);
                grades.add(el.getTextContent());
            }
            data.put("grades", grades);

            // Připomínky
            List<String> reminders = new ArrayList<>();
            NodeList reminderNodes = rootElement.getElementsByTagName("reminder");
            for (int i = 0; i < reminderNodes.getLength(); i++) {
                Element el = (Element) reminderNodes.item(i);
                reminders.add(el.getTextContent());
            }
            data.put("reminders", reminders);

            System.out.println("✓ Data importována z: " + filePath);
        } catch (Exception e) {
            System.err.println("✗ Chyba při importu: " + e.getMessage());
            e.printStackTrace();
        }

        return data;
    }
}

