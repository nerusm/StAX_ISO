package suren;

import static java.nio.file.StandardOpenOption.*;
import javax.xml.stream.*;
import javax.xml.stream.events.XMLEvent;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Stack;
import java.util.concurrent.TimeUnit;
import java.nio.channels.FileChannel;

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

    public static void mergeFiles(String headerFile, String batchFile, String outputFile) throws IOException {
        Path outFile = Paths.get(outputFile);
        String[] arg = new String[]{headerFile, batchFile};
        try (FileChannel out = FileChannel.open(outFile, CREATE, WRITE)) {
            for (int ix = 0, n = arg.length; ix < n; ix++) {
                Path inFile = Paths.get(arg[ix]);
                System.out.println(inFile + "...");
                try (FileChannel in = FileChannel.open(inFile, READ)) {
                    for (long p = 0, l = in.size(); p < l; )
                        p += in.transferTo(p, l - p, out);
                }
            }
        }

    }

    public static void main(String args[]) throws IOException, XMLStreamException {
        long startTime = System.nanoTime();
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
        String HEADER_TOTAL_AMT_TAG = "/Document/pain.001.001.02/GrpHdr/CtrlSum";
        String HEADER_TOTAL_NO_OF_BATCHES = "/Document/pain.001.001.02/GrpHdr/NbOfTxs";
        int totalNoOfTxns = 0;

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
                                totalNoOfTxns++;
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

            openTags = new Stack<>();

            FileWriter fileWriter =
                    new FileWriter("/Users/suren/IdeaProjects/StAX_ISO/Resources/out1_v2_header.xml");
            XMLEventWriter headerWriter = XMLOutputFactory.newInstance().createXMLEventWriter(fileWriter);

            boolean isAmtTag = false;
            boolean isTotalTag = false;
            XMLEventFactory eventFactory = XMLEventFactory.newInstance();
            String totalAmount = "13453500";
            String totalTxns = "2509";
            for (int i = 0; i < headerArrayList.size(); i++) {
                XMLEvent eve = headerArrayList.get(i);

                if (eve.isStartElement()) {
                    openTags.push(eve.asStartElement().getName().getLocalPart());
                    if (HEADER_TOTAL_NO_OF_BATCHES.equalsIgnoreCase(formXPath(openTags))) {
                        System.out.println("No of txns tag: " + formXPath(openTags) + "-" + headerArrayList.get(i + 1));
                        isTotalTag = true;
                    } else if (HEADER_TOTAL_AMT_TAG.equalsIgnoreCase(formXPath(openTags))) {
                        System.out.println("Amt  tag: " + formXPath(openTags));
                        isAmtTag = true;
                    }
                } else if (eve.isEndElement()) {
                    openTags.pop();
                }
                //if(!isAmtTag && !isTotalTag) {
                headerWriter.add(eve);
                //} else
                if (isAmtTag) {
                    headerWriter.add(eventFactory.createCharacters(totalAmount));
                    i = i + 1;
                    isAmtTag = false;
                } else if (isTotalTag) {
                    headerWriter.add(eventFactory.createCharacters(totalNoOfTxns + ""));
                    i = i + 1;
                    isTotalTag = false;
                }
            }


            Writer trailerWriter = new BufferedWriter(
                    new FileWriter("/Users/suren/IdeaProjects/StAX_ISO/Resources/out1_v2_batches.xml", true));
            for (int i = 0; i < trailerArrayList.size(); i++) {
                trailerWriter.write(formTags(trailerArrayList.get(i)));
            }

            headerWriter.flush();
            trailerWriter.flush();
            long elapsedTime = System.nanoTime() - startTime;

            mergeFiles("/Users/suren/IdeaProjects/StAX_ISO/Resources/out1_v2_header.xml", "/Users/suren/IdeaProjects/StAX_ISO/Resources/out1_v2_batches.xml", "/Users/suren/IdeaProjects/StAX_ISO/Resources/FinalOut.xml");

            System.out.println(TimeUnit.SECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS));



        } catch (XMLStreamException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }
}
