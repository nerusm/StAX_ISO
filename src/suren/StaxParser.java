package suren;

import javax.xml.stream.*;
import javax.xml.stream.events.XMLEvent;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Stack;

/**
 * Created by suren on 10/7/15.
 */
public class StaxParser {


    XMLEventFactory m_eventFactory = XMLEventFactory.newInstance();

    public static String formXPath(Stack<String> stack){
        Iterator<String> i = stack.iterator();
        String tempXpath = "";

        while(i.hasNext()){
            String s = i.next();
            tempXpath = tempXpath.concat("/").concat(s);
        }

        //////System.out.println(tempXpath);
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

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        ArrayList<XMLEvent> batchStartArrayList = new ArrayList<>();
        Stack<String> arrayList = new Stack<>();
        boolean isHeaderOpen = true;
        boolean isBatchStart = false;
        boolean isSUAbatch = false;
        String BATCH_START_ID = "/Document/pain.001.001.02/PmtInf";
        String SUA_IDENTIFIER_xPATH = "/Document/pain.001.001.02/PmtInf/Dbtr/Nm";

        XMLEventWriter writer = null ;
        if(args.length < 1){
            ////System.out.println("Usage: Specify XML File Name");
        }
        try{
            StaxParser ms = new StaxParser();

            XMLEventReader reader = XMLInputFactory.newInstance().createXMLEventReader(new
                    java.io.FileInputStream("/Users/suren/IdeaProjects/StAX_ISO/Resources/pain.001.001.02_S.xml"));
            writer = XMLOutputFactory.newInstance().createXMLEventWriter(
                    new FileWriter("/Users/suren/IdeaProjects/StAX_ISO/Resources/out1.xml"));

            while(reader.hasNext()){
                XMLEvent event = (XMLEvent)reader.next();
                if(event.isStartElement()){
                    arrayList.push(event.asStartElement().getName().getLocalPart());
                   //formXPath(arrayList);
                    if(BATCH_START_ID.equalsIgnoreCase(formXPath(arrayList))){
                        isBatchStart = true;
                        isHeaderOpen = false;
                        isSUAbatch = false;
                        //System.out.println("S: "+formXPath(arrayList));
                    }
                   // ////System.out.println("S: "+event.asStartElement().getName().getLocalPart());
                }

                if(event.isEndElement()){
                    if(BATCH_START_ID.equalsIgnoreCase(formXPath(arrayList))){
                        isSUAbatch = false;
                       // isHeaderOpen = false;
                        //isBatchStart = false;
                        //System.out.println("E: "+formXPath(arrayList));
                        writer.add(event);
                    }
                }

                if(isSUAbatch){
                    writer.add(event);
                }
                if(isHeaderOpen) {
                    System.out.println(formXPath(arrayList) + "-" + getEventTypeString(event.getEventType()));
                    writer.add(event);
                }
                if(isBatchStart){
                    batchStartArrayList.add(event);
                    //System.out.println("H not open: "+event.toString());
                }
                if(event.isEndElement()){
                    arrayList.pop();
                    ////System.out.println("E: " + event.asEndElement().getName().getLocalPart());
                }
                if(formXPath(arrayList).equalsIgnoreCase(SUA_IDENTIFIER_xPATH)){
                   // event = reader.peek();
                    if(reader.peek().isCharacters()){
                        String catgryPurpCode = reader.peek().asCharacters().getData();
                        if(catgryPurpCode.equalsIgnoreCase("CCRD")){
                            isSUAbatch = true;
                            //isBatchStart = false;
                         //  writer.add(event);
                            for (int i = 0; i < batchStartArrayList.size(); i++) {
                                ////System.out.println(batchStartArrayList.get(i));
                                writer.add(batchStartArrayList.get(i));
                            }
                            batchStartArrayList.removeAll(batchStartArrayList);
                        }
                        ////System.out.println(catgryPurpCode);
                    }
                }


            }

            writer.flush();

            ////System.out.println(arrayList.size());



        }catch(Exception ex){

            ex.printStackTrace();
        }

        finally {
            try {
                writer.flush();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
