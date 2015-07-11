package suren;

import javax.xml.stream.*;
import javax.xml.stream.events.XMLEvent;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Stack;

/**
 * Created by suren on 11/7/15.
 */
public class StAX_ParserV2 {

    XMLEventFactory m_eventFactory = XMLEventFactory.newInstance();

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

    public static void main(String args[]) throws IOException {
        ArrayList<XMLEvent> preSUAbatchEventsArrayList = new ArrayList<>();
        Stack<String> openTags = new Stack<String>();
        boolean isHeaderOpen = true;
        boolean isBatchOpen = false;
        boolean isSUAbatch = false;
        String BATCH_START_TAG_XPATH = "/Document/pain.001.001.02/PmtInf";
        String SUA_IDENTIFIER_xPATH = "/Document/pain.001.001.02/PmtInf/Dbtr/Nm";

        XMLEventWriter writer = null;

        try {
            XMLEventReader reader = XMLInputFactory.newInstance().createXMLEventReader(new
                    java.io.FileInputStream("/Users/suren/IdeaProjects/StAX_ISO/Resources/pain.001.001.02.xml"));
            writer = XMLOutputFactory.newInstance().createXMLEventWriter(
                    new FileWriter("/Users/suren/IdeaProjects/StAX_ISO/Resources/out1_v2.xml"));

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
                                    writer.add(aPreSUAbatchEventsArrayList);
                                }

                                //*** Reset the preSUAbatchevents
                                final boolean b = preSUAbatchEventsArrayList.removeAll(preSUAbatchEventsArrayList);
                            }
                        }
                    }
                }

                if (isHeaderOpen || isSUAbatch || !isBatchOpen) {
                    writer.add(event);
                } else if (isBatchOpen && !isSUAbatch) {
                    preSUAbatchEventsArrayList.add(event);
                }

                if (event.isEndElement()) {
                    if (BATCH_START_TAG_XPATH.equalsIgnoreCase(formXPath(openTags))) {
                        isBatchOpen = false;
                        isSUAbatch = false;
                        preSUAbatchEventsArrayList.clear();
                    }
                    openTags.pop();
                }
            }

            writer.flush();


        } catch (XMLStreamException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }
}
