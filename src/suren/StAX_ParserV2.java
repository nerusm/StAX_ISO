package suren;

import javax.xml.stream.*;
import javax.xml.stream.events.XMLEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Stack;

/**
 * Created by suren on 11/7/15.
 */
public class StAX_ParserV2 {

    //XMLEventFactory m_eventFactory = XMLEventFactory.newInstance();

    public static String formXPath(Stack<String> stack) {
        Iterator<String> i = stack.iterator();
        String tempXpath = "";
        while (i.hasNext()) {
            String s = i.next();
            tempXpath = tempXpath.concat("/").concat(s);
        }
        return tempXpath;
    }

    public static final String getEventTypeString(int eventType) {
        switch (eventType) {
            case XMLEvent.START_ELEMENT:
                return "START_ELEMENT";

            case XMLEvent.END_ELEMENT:
                return "END_ELEMENT";

            case XMLEvent.PROCESSING_INSTRUCTION:
                return "PROCESSING_INSTRUCTION";

            case XMLEvent.CHARACTERS:
                return "CHARACTERS";

            case XMLEvent.COMMENT:
                return "COMMENT";

            case XMLEvent.START_DOCUMENT:
                return "START_DOCUMENT";

            case XMLEvent.END_DOCUMENT:
                return "END_DOCUMENT";

            case XMLEvent.ENTITY_REFERENCE:
                return "ENTITY_REFERENCE";

            case XMLEvent.ATTRIBUTE:
                return "ATTRIBUTE";

            case XMLEvent.DTD:
                return "DTD";

            case XMLEvent.CDATA:
                return "CDATA";
        }

        return "UNKNOWN_EVENT_TYPE";
    }

    public static String formTags(XMLEvent event) {
        switch (event.getEventType()) {
            case XMLEvent.START_ELEMENT:
                return "<" + event.asStartElement().getName().getLocalPart() + ">";

            case XMLEvent.END_ELEMENT:
                return "</" + event.asEndElement().getName().getLocalPart() + ">";

            case XMLEvent.PROCESSING_INSTRUCTION:
                return "PROCESSING_INSTRUCTION";

            case XMLEvent.CHARACTERS:
                return event.asCharacters().getData();

            case XMLEvent.END_DOCUMENT:
                return "";

            case XMLEvent.COMMENT:
                return event.toString();


        }
        return null;
    }

    public static void main(String args[]) throws IOException, XMLStreamException {
        ArrayList<XMLEvent> preSUAbatchEventsArrayList = new ArrayList<>();
        ArrayList<XMLEvent> headerArrayList = new ArrayList<XMLEvent>();
        ArrayList<XMLEvent> trailerArrayList = new ArrayList<XMLEvent>();
        Stack<String> openTags = new Stack<String>();
        boolean isHeaderOpen = true;
        boolean isBatchOpen = false;
        boolean isSUAbatch = false;
        boolean isTrailerStart = false;
        String BATCH_START_TAG_XPATH = "/Document/pain.001.001.02/PmtInf";
        String SUA_IDENTIFIER_xPATH = "/Document/pain.001.001.02/PmtInf/Dbtr/Nm";
        String TRAILER_START_xPATH = "/Document/pain.001.001.02/GrpHdr";

        XMLEventWriter batchWriter = null;

        try {
            XMLEventReader reader = XMLInputFactory.newInstance().createXMLEventReader(new
                    java.io.FileInputStream("/Users/suren/IdeaProjects/StAX_ISO/Resources/pain.001.001.02.xml"));
            batchWriter = XMLOutputFactory.newInstance().createXMLEventWriter(
                    new FileWriter("/Users/suren/IdeaProjects/StAX_ISO/Resources/out1_v2_batches.xml"));

            while (reader.hasNext()) {

                XMLEvent event = (XMLEvent) reader.next();
                if (event.isStartElement()) {
                    //*** Add the opening tag to the openTag Stack
                    openTags.push(event.asStartElement().getName().getLocalPart());

                    //*** Check if Header over and Batches started
                    if (BATCH_START_TAG_XPATH.equalsIgnoreCase(formXPath(openTags))) {
                        isBatchOpen = true;
                        isHeaderOpen = false;
                    }

                    if (SUA_IDENTIFIER_xPATH.equalsIgnoreCase(formXPath(openTags))) {
                        if (reader.peek().isCharacters()) {
                            String catgryPurpCode = reader.peek().asCharacters().getData();
                            if (catgryPurpCode.equalsIgnoreCase("CCRD")) {
                                isSUAbatch = true;
                                for (XMLEvent aPreSUAbatchEventsArrayList : preSUAbatchEventsArrayList) {
                                    batchWriter.add(aPreSUAbatchEventsArrayList);
                                }

                                //*** Reset the preSUAbatchevents
                                final boolean b = preSUAbatchEventsArrayList.removeAll(preSUAbatchEventsArrayList);
                            }
                        }
                    }
                }

                if ((isSUAbatch || !isBatchOpen) && (!isHeaderOpen && isBatchOpen)) {
                    batchWriter.add(event);
                } else if (isBatchOpen && !isSUAbatch) {
                    preSUAbatchEventsArrayList.add(event);
                } else if (!isTrailerStart) {
                    headerArrayList.add(event);
                } else if (isTrailerStart) {
                    trailerArrayList.add(event);
                }

                if (event.isEndElement()) {
                    if (BATCH_START_TAG_XPATH.equalsIgnoreCase(formXPath(openTags))) {
                        isBatchOpen = false;
                        isSUAbatch = false;
                        preSUAbatchEventsArrayList.clear();
                    }
                    if (TRAILER_START_xPATH.equalsIgnoreCase(formXPath(openTags))) {
                        System.out.println("*************** Start of trailer");
                        isTrailerStart = true;
                    }
                    openTags.pop();
                }
            }

            batchWriter.flush();

            FileWriter fileWriter =
                    new FileWriter("/Users/suren/IdeaProjects/StAX_ISO/Resources/out1_v2_header.xml");
            XMLEventWriter headerWriter = XMLOutputFactory.newInstance().createXMLEventWriter(fileWriter);
            for (int i = 0; i < headerArrayList.size(); i++) {
                XMLEvent eve = headerArrayList.get(i);
                headerWriter.add(eve);
            }


            Writer trailerWriter = new BufferedWriter(
                    new FileWriter("/Users/suren/IdeaProjects/StAX_ISO/Resources/out1_v2_batches.xml", true));
            for (int i = 0; i < trailerArrayList.size(); i++) {
                trailerWriter.write(formTags(trailerArrayList.get(i)));
            }

            headerWriter.flush();
            trailerWriter.flush();

        } catch (XMLStreamException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }
}
