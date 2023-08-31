package app.cargoflow;

import app.cargoflow.Model.Bag;
import app.cargoflow.Model.Order;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.javalite.activejdbc.*;
import org.javalite.activejdbc.connection_config.ConnectionJdbcSpec;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static app.cargoflow.Model.Order.findFirst;
import static org.slf4j.LoggerFactory.getLogger;

@SpringBootApplication
public class FailedOrdersCargoflow implements CommandLineRunner {

    private static final Logger log = getLogger(FailedOrdersCargoflow.class);
    private static final String FILE_NAME = "D:/Failed Orders Results/failed_orders.xlsx";


    XSSFWorkbook failedorders = new XSSFWorkbook();
    XSSFSheet sheetFullBag = failedorders.createSheet("pickup failed in FullBag");
    XSSFSheet sheetNotFullBag = failedorders.createSheet("pickup failed in Not FullBag");
    XSSFSheet sheetHoOutFullBag = failedorders.createSheet("pickupHoOut failed in FullBag");
    XSSFSheet sheetHoOutNotFullBag = failedorders.createSheet("pickupHoOut failed in Not FullBag");

    XSSFFont font = failedorders.createFont();
    XSSFCellStyle style = failedorders.createCellStyle();

    int row_pfFb = 0;
    int row_pfNfb = 0;
    int row_pHOfFb = 0;
    int row_pHOfNfb = 0;



    public static void main(String[] args) {
        SpringApplication.run(FailedOrdersCargoflow.class, args);
    }
    @Override
    public void run(String... args) throws Exception {
        List<Order>pickupfailedFullBag = new ArrayList<>();
        List<Order>pickupfailedNotFullBag = new ArrayList<>();
        List<Order>pickupHoOutfailedFullBag = new ArrayList<>();
        List<Order>pickupHoOutfailedNotFullBag = new ArrayList<>();

        font.setBold(true);
        style.setBorderTop(BorderStyle.THIN);
        style.setFont(font);

        headers();

        log.info("Started");
        Base.open();
        log.info("Connected");

        Configuration config = Registry.instance().getConfiguration();
        ConnectionJdbcSpec spec = (ConnectionJdbcSpec) config.getCurrentConnectionSpec();
        if(spec == null){
            throw new DBException("Could not find configuration in a property file for environment: " + config.getEnvironment() +
                    ". Are you sure you have a database.properties file configured?");
        }

        log.info("Starting to synchronize shipments with reported copies...");
        List<String> pickupfailed = Base.firstColumn("select tq.correlation_id " +
                "from task_queue tq where tq.create_time > now() - interval '10 day' " +
                "and tq.actor = 'CAINIAO_GLOBAL_PICKUP_CALLBACK' and tq.status = 'FAIL'");
        List<String> pickuphooutfailed = Base.firstColumn("select tq.correlation_id " +
                "from task_queue tq where tq.create_time > now() - interval '10 day' " +
                "and tq.actor = 'CAINIAO_GLOBAL_PICKUP_HOOUT_CALLBACK' and tq.status = 'FAIL'");
        List<Order> orderpickupfailed =Order.findBySQL("select o.* from \"order\" o left outer join  task_queue tq  on tq.correlation_id =  o.logistics_order_code where tq.create_time > now() - interval '30 day' and tq.actor = 'CAINIAO_GLOBAL_PICKUP_CALLBACK' and tq.status = 'FAIL'");
        List<Order> orderpickupHooutfailed =Order.findBySQL("select o.* from \"order\" o left outer join  task_queue tq  on tq.correlation_id =  o.logistics_order_code where tq.create_time > now() - interval '30 day' and tq.actor = 'CAINIAO_GLOBAL_PICKUP_HOOUT_CALLBACK' and tq.status = 'FAIL'");


        for (Order item: orderpickupfailed){}
        List<Bag> bagpickup = Bag.findBySQL("select  * from bag  where big_bag_id in (select o.big_bag_id from \"order\" o left outer join  task_queue tq  on tq.correlation_id =  o.logistics_order_code where tq.create_time > now() - interval '30 day' and tq.actor = 'CAINIAO_GLOBAL_PICKUP_CALLBACK' and tq.status = 'FAIL')");
        List<Bag> bagpickupHoout = Bag.findBySQL("select  * from bag  where big_bag_id in (select o.big_bag_id from \"order\" o left outer join  task_queue tq  on tq.correlation_id =  o.logistics_order_code where tq.create_time > now() - interval '30 day' and tq.actor = 'CAINIAO_GLOBAL_PICKUP_HOOUT_CALLBACK' and tq.status = 'FAIL')");
        System.out.println("pickup failed orders");
        for(Bag item: bagpickup){
            List<Order>list = new ArrayList<>();
            for(Order o: orderpickupfailed) if (item.getBigBagId().equals(o.getBigBagId())){list.add(o);}
            System.out.println(item.getOrderNumber()+" vs "+list.size()+" in bag "+item.getBigBagId());
            if(item.getOrderNumber()==list.size()){pickupfailedFullBag.addAll(list);}
            else {pickupfailedNotFullBag.addAll(list);}
        }
        System.out.println("pickup hoout failed orders");
        for(Bag item: bagpickupHoout){
            List<Order>list = new ArrayList<>();
            for(Order o: orderpickupHooutfailed) if (item.getBigBagId().equals(o.getBigBagId())){list.add(o);}
            System.out.println(item.getOrderNumber()+" vs "+list.size()+" in bag "+item.getBigBagId());
            if(item.getOrderNumber()==list.size()){pickupHoOutfailedFullBag.addAll(list);}
            else {pickupHoOutfailedNotFullBag.addAll(list);}
        }



        System.out.println("total quantity of failed orders = "+orderpickupfailed.size()+" and "+orderpickupHooutfailed.size());
        System.out.println("quantity of pickupfailedFullBag = "+pickupfailedFullBag.size());
        System.out.println("quantity of pickupfailedNotFullBag = "+pickupfailedNotFullBag.size());
       System.out.println("quantity of pickupHoOutfailedFullBag = "+pickupHoOutfailedFullBag.size());
        System.out.println("quantity of pickupHoOutfailedNotFullBag = "+pickupHoOutfailedNotFullBag.size());
//        System.out.println("quantity of hoout elements = "+orderpickuphooutfailed.size());
       //   for (Order item:orderpickupfailed){System.out.println(item.getLogisticsOrderCode());}
//        for (Bag item:bagpickup){System.out.println(item);}
       // List<ShipmentReporting> shipmentsToProcess = ShipmentReporting.find("departure BETWEEN '"+FROM+"' AND '"+TO+"' AND (report_id is null OR report_id= ?)",reportId);
//        log.info("Got {} shipments to process", shipmentsToProcess.size());

//        int sumOfPieces = 0;
//        double sumOfGrossWeight = 0;
//        double sumOftotalAmouemnt = 0;
//        int lastReportedProgress = 0, processed = 0;
//        for (ShipmentReporting shipmentReporting : shipmentsToProcess) {
//            String transportDocument = shipmentReporting.getDocument();
//            List<Map> linehauls = Base.findAll("SELECT DISTINCT name FROM channels WHERE id IN (SELECT  channel_id FROM bag_reporting WHERE shipment_id= ?)", shipmentReporting.getId());
//            String linehaulCode = "";
//            for (Map linehaul : linehauls) {
//                if (linehauls.indexOf(linehaul) > 0) {
//                    linehaulCode += ", ";
//                }
//                linehaulCode += (String) linehaul.get("name");
//            }
//
//            LocalDateTime aTD = shipmentReporting.getDeparture().atZone(ZoneId.of("UTC+0")).withZoneSameInstant(ZoneId.of("UTC+8")).toLocalDateTime();
//            String flight1 = shipmentReporting.getFlight();
//            String freightAgent = (String) Base.firstCell("SELECT freight_agent FROM shipments WHERE id= ?", shipmentReporting.getId());
//            String placeOfOrigin = (String) Base.firstCell("SELECT iata FROM offices WHERE id= ?", shipmentReporting.getOfficeId());
//            String departureIATACode = shipmentReporting.getDepartureAirport();
//            String destinationIATACode = shipmentReporting.getArrivalAirport();
//            String sellingProduct = shipmentReporting.getBookingType();
//            int pieces = shipmentReporting.getBagCount();
//            sumOfPieces += pieces;
//            double grossWeight;
//            Object trialGrossWeight = Base.firstCell("SELECT awb_weight FROM shipments WHERE id= ?", shipmentReporting.getId());
//            if (trialGrossWeight == null) {
//                grossWeight = shipmentReporting.getPostalWeight();
//            } else {
//                grossWeight = ((BigDecimal) trialGrossWeight).doubleValue();
//            }
//            sumOfGrossWeight += grossWeight;
//
////            List<Map> rates = Base.findAll("SELECT rates::json->>'trucking' trucking, rates::json->>'chinaMlHandling' chinaMlHandling, rates::json->>'additionalWhHandling' additionalWhHandling, " +
////                            "rates::json->>'handlingShipmBatteries' handlingShipmBatteries, rates::json->>'terminalHandling' terminalHandling, rates::json->>'customClearance' customClearance, " +
////                            "rates::json->>'tHCOrigin' tHCOrigin, rates::json->>'tHCDestination' tHCDestination, rates::json->>'linehaulService' linehaulService, rates::json->>'linehaul' linehaul\n" +
////                            "FROM  rates WHERE port_of_loading = ? AND port_of_departure = ? AND port_of_destination = ? AND booking_type = ? AND ? = ANY(channels)  AND valid_from <='" + FROM + "' AND freight_agent= ? ORDER BY valid_from DESC LIMIT 1",
////                    placeOfOrigin, departureIATACode, destinationIATACode, sellingProduct, shipmentReporting.getChannelId(),freightAgent);
//
//            List<Map> rates = Base.findAll("SELECT rates::json->>'trucking' trucking, rates::json->>'chinaMlHandling' chinaMlHandling, rates::json->>'additionalWhHandling' additionalWhHandling, " +
//                            "rates::json->>'handlingShipmBatteries' handlingShipmBatteries, rates::json->>'terminalHandling' terminalHandling, rates::json->>'customClearance' customClearance, " +
//                            "rates::json->>'tHCOrigin' tHCOrigin, rates::json->>'tHCDestination' tHCDestination, rates::json->>'linehaulService' linehaulService, rates::json->>'linehaul' linehaul\n" +
//                            "FROM  rates WHERE port_of_loading = ? AND ? = ANY(port_of_departure) AND ? = ANY(port_of_destination)  AND ? = ANY(channels)  AND valid_from <='" + FROM + "' AND freight_agent= ? ORDER BY valid_from DESC LIMIT 1",
//                    placeOfOrigin, departureIATACode, destinationIATACode, shipmentReporting.getChannelId(),freightAgent);
//
//            System.out.println(shipmentReporting.getId()+"  "+placeOfOrigin + " " + departureIATACode + " " + destinationIATACode + " " + shipmentReporting.getChannelId()+" "+freightAgent);
//
//            double truckRate = 0;
//            double chinaMainlandHandlingRate = 0;
//            double addWHHandlingRate = 0;
//            double handlingShipmWithBatteriesRate = 0;
//            double terminalHandlingRate = 0;
//            double customClearanceRate = 0;
//            double tHCOriginRate = 0;
//            double tHCDestinationRate = 0;
//            double linehaulServiceRate = 0;
//            double linehaulRate = 0;
//
//            if (rates.size() > 0) {
//                truckRate = Double.parseDouble((String) rates.get(0).get("trucking"));
//                chinaMainlandHandlingRate = Double.parseDouble((String) rates.get(0).get("chinaMlHandling"));
//                addWHHandlingRate = Double.parseDouble((String) rates.get(0).get("additionalWhHandling"));
//                handlingShipmWithBatteriesRate = Double.parseDouble((String) rates.get(0).get("handlingShipmBatteries"));
//                terminalHandlingRate = Double.parseDouble((String) rates.get(0).get("terminalHandling"));
//                customClearanceRate = Double.parseDouble((String) rates.get(0).get("customClearance"));
//                tHCOriginRate = Double.parseDouble((String) rates.get(0).get("tHCOrigin"));
//                tHCDestinationRate = Double.parseDouble((String) rates.get(0).get("tHCDestination"));
//                linehaulServiceRate = Double.parseDouble((String) rates.get(0).get("linehaulService"));
//                linehaulRate = Double.parseDouble((String) rates.get(0).get("linehaul"));
//            }
//
//            if (sellingProduct.equals("FCL_FTL")) {
//                seadata((Integer) shipmentReporting.getId(),transportDocument,placeOfOrigin, departureIATACode, destinationIATACode, sellingProduct,freightAgent, aTD,terminalHandlingRate,linehaulRate);
//            } else {
//                if (truckRate != 0) {
//                    truckdata((Integer) shipmentReporting.getId(), transportDocument,freightAgent, placeOfOrigin, departureIATACode, destinationIATACode,truckRate);
//                }
//
//
//                double truckAmount = (double) Math.round(truckRate * grossWeight * 100) / 100;
//                double chinaMainlandHandlingAmount = (double) Math.round(chinaMainlandHandlingRate * grossWeight * 100) / 100;
//                double addWHHandlingAmount = (double) Math.round(addWHHandlingRate * grossWeight * 100) / 100;
//                double handlingShipmWithBatteriesAmount = (double) Math.round(handlingShipmWithBatteriesRate * grossWeight * 100) / 100;
//                double terminalHandlingAmount = (double) Math.round(terminalHandlingRate * grossWeight * 100) / 100;
//                double customClearanceAmount = (double) Math.round(customClearanceRate * grossWeight * 100) / 100;
//                double tHCOriginAmount = (double) Math.round(tHCOriginRate * grossWeight * 100) / 100;
//                double tHCDestinationAmount = (double) Math.round(tHCDestinationRate * grossWeight * 100) / 100;
//                double linehaulServiceAmount = (double) Math.round(linehaulServiceRate * grossWeight * 100) / 100;
//                double linehaulAmount = (double) Math.round(linehaulRate * grossWeight * 100) / 100;
//
//                double totalRate = chinaMainlandHandlingRate + addWHHandlingRate + handlingShipmWithBatteriesRate + terminalHandlingRate + customClearanceRate + tHCOriginRate + tHCDestinationRate + linehaulServiceRate + linehaulRate;
//                double totalAmount = chinaMainlandHandlingAmount + addWHHandlingAmount + handlingShipmWithBatteriesAmount + terminalHandlingAmount + customClearanceAmount + tHCOriginAmount + tHCDestinationAmount + linehaulServiceAmount + linehaulAmount;
//                sumOftotalAmount += totalAmount;
//
//                Object[] rowFill = {transportDocument, linehaulCode,freightAgent, aTD, flight1, placeOfOrigin, departureIATACode, destinationIATACode, sellingProduct, pieces, grossWeight, totalRate, totalAmount,
//                        chinaMainlandHandlingRate, chinaMainlandHandlingAmount, addWHHandlingRate, addWHHandlingAmount, handlingShipmWithBatteriesRate, handlingShipmWithBatteriesAmount, terminalHandlingRate,
//                        terminalHandlingAmount, customClearanceRate, customClearanceAmount, tHCOriginRate, tHCOriginAmount, tHCDestinationRate, tHCDestinationAmount, linehaulServiceRate, linehaulServiceAmount,
//                        linehaulRate, linehaulAmount};
//
//                Row row = mainSheet.createRow(rowNum++);
//                int colNum = 0;
//                for (Object field : rowFill) {
//                    Cell cell = row.createCell(colNum++);
//                    if (field instanceof String) {
//                        cell.setCellValue((String) field);
//                    } else if (field instanceof Integer) {
//                        cell.setCellValue((Integer) field);
//                    } else if (field instanceof Double) {
//                        cell.setCellValue((Double) field);
//                    } else if (field instanceof Timestamp) {
//                        cell.setCellValue((Timestamp) field);
//                    } else if (field instanceof LocalDateTime) {
//                        cell.setCellValue(String.valueOf((LocalDateTime) field));
//                    }
//                }
//            }
//            shipmentReporting.setReportId(reportId); //Marking reported shipments
//            shipmentReporting.save();
//
//            processed++;
//            if (processed * 100 / shipmentsToProcess.size() > lastReportedProgress) {
//                lastReportedProgress = processed * 100 / shipmentsToProcess.size();
//                log.info("{}% processed {} shipments of {}", lastReportedProgress, processed, shipmentsToProcess.size());
//            }
//        }

//        Object[] rowTotal = {"TOTAL","","","","","","","","",sumOfPieces,sumOfGrossWeight,"",sumOftotalAmount};
//        Row row = mainSheet.createRow(rowNum++);
//        int colNum = 0;
//        for (Object field : rowTotal) {
//            Cell cell = row.createCell(colNum++);
//            cell.setCellStyle(style);
//            if (field instanceof String) {
//                cell.setCellValue((String) field);
//            } else if (field instanceof Integer) {
//                cell.setCellValue((Integer) field);
//            } else if (field instanceof Double) {
//                cell.setCellValue((Double) field);
//            }
//        }

        sheetFullBag.setColumnWidth(0,5000);sheetFullBag.setColumnWidth(1,5000);sheetFullBag.setColumnWidth(2,4000);sheetFullBag.setColumnWidth(3,4000);
        sheetFullBag.setColumnWidth(4,4000);sheetFullBag.setColumnWidth(5,4000);sheetFullBag.setColumnWidth(6,4000);sheetFullBag.setColumnWidth(7,5000);

        sheetNotFullBag.setColumnWidth(0,5000);sheetNotFullBag.setColumnWidth(1,5000);sheetNotFullBag.setColumnWidth(2,4000);sheetNotFullBag.setColumnWidth(3,4000);
        sheetNotFullBag.setColumnWidth(4,4000);sheetNotFullBag.setColumnWidth(5,4000);sheetNotFullBag.setColumnWidth(6,4000);sheetNotFullBag.setColumnWidth(7,5000);

        sheetHoOutFullBag.setColumnWidth(0,5000);sheetHoOutFullBag.setColumnWidth(1,5000);sheetHoOutFullBag.setColumnWidth(2,4000);sheetHoOutFullBag.setColumnWidth(3,4000);
        sheetHoOutFullBag.setColumnWidth(4,4000);sheetHoOutFullBag.setColumnWidth(5,4000);sheetHoOutFullBag.setColumnWidth(6,4000);sheetHoOutFullBag.setColumnWidth(7,5000);

        sheetHoOutNotFullBag.setColumnWidth(0,5000);sheetHoOutNotFullBag.setColumnWidth(1,5000);sheetHoOutNotFullBag.setColumnWidth(2,4000);sheetHoOutNotFullBag.setColumnWidth(3,4000);
        sheetHoOutNotFullBag.setColumnWidth(4,4000);sheetHoOutNotFullBag.setColumnWidth(5,4000);sheetHoOutNotFullBag.setColumnWidth(6,4000);sheetHoOutNotFullBag.setColumnWidth(7,5000);



        try {
            FileOutputStream outputStream = new FileOutputStream(FILE_NAME);
            failedorders.write(outputStream);
            failedorders.close();
            outputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Base.close(true);
    }

    public void headers(){
        Object[] datatype = {"logistics_order_code", "tracking_number","big_bag_id", "provider_id", "created_at", "reference_id", "lastmile_id", "task queue response"};

            Row row_Fb = sheetFullBag.createRow(row_pfFb++);
            Row row_nFb = sheetNotFullBag.createRow(row_pfNfb++);
            Row row_HOFb = sheetHoOutFullBag.createRow(row_pHOfFb++);
            Row row_HONfb = sheetHoOutNotFullBag.createRow(row_pHOfNfb++);
            int colNum = 0;
            for (Object field : datatype) {
                Cell cell1 = row_Fb.createCell(colNum);
                Cell cell2 = row_nFb.createCell(colNum);
                Cell cell3 = row_HOFb.createCell(colNum);
                Cell cell4 = row_HONfb.createCell(colNum);
                colNum++;
                if (field instanceof String) {
                    cell1.setCellValue((String) field);
                    cell2.setCellValue((String) field);
                    cell3.setCellValue((String) field);
                    cell4.setCellValue((String) field);
                }
            }
    }
    public void truckdata(int shipmentId,String transportDocument,String freightAgent,String placeOfOrigin,String departureIATACode,String destinationIATACode,double rate){
//        LocalDateTime time = null;
//        List<Map> bags = Base.findAll("SELECT  big_bag_id FROM bag_reporting WHERE shipment_id= ?",shipmentId);
//        for (Map bag:bags) {
//            String bbId=(String) bag.get("big_bag_id");
//            BigDecimal weight = (BigDecimal) Base.firstCell("SELECT declared_weight FROM bag_reporting WHERE big_bag_id= ?", bbId);
//            transportDocument= (String) Base.firstCell("SELECT document FROM bag_reporting WHERE big_bag_id= ?",bbId);
//            //    String lhname = (String) Base.firstCell("SELECT name FROM channels WHERE id IN (SELECT  channel_id FROM bag_reporting WHERE big_bag_id= ?)", bbId);
//            Timestamp stamp = ((Timestamp) Base.firstCell("SELECT outbound FROM bag_reporting WHERE big_bag_id= ?", bbId));
//            if (stamp != null) { time = stamp.toLocalDateTime().atZone(ZoneId.of("UTC+0")).withZoneSameInstant(ZoneId.of("UTC+8")).toLocalDateTime();}
//            double gross = ((double) Math.round(weight.doubleValue()*rate*100))/100;
//            Object [] rowFill = {transportDocument, bbId,freightAgent,weight.doubleValue(),placeOfOrigin,departureIATACode,destinationIATACode,time,rate,gross};
//
//            Row row = truckSheet.createRow(rownumtruck++);//System.out.println("rownumtruck "+rownumtruck);
//            int colNum = 0;
//            for (Object field : rowFill) {
//                Cell cell = row.createCell(colNum++);
//                if (field instanceof String) {
//                    cell.setCellValue((String) field);
//                } else if (field instanceof Integer) {
//                    cell.setCellValue((Integer) field);
//                } else if (field instanceof BigDecimal) {
//                    cell.setCellValue((Double) field);
//                }else if (field instanceof Double) {
//                    cell.setCellValue((Double) field);
//                } else if (field instanceof Timestamp) {
//                    cell.setCellValue(String.valueOf((Timestamp) field));
//                } else if (field instanceof LocalDateTime) {
//                    cell.setCellValue(String.valueOf((LocalDateTime) field));
//                }
//            }
//        };
    }
    public void seadata(int shipmentId,String transportDocument,String placeOfOrigin,String departureIATACode,String destinationIATACode,String sellingProduct,String freightAgent,LocalDateTime time,double handgilgRate,double linehaulFee){
////        LocalDateTime time = null;
//        List<Map> bags = Base.findAll("SELECT  big_bag_id FROM bag_reporting WHERE shipment_id= ?",shipmentId);
//        for (Map bag:bags) {
//            String bbId=(String) bag.get("big_bag_id");
//            BigDecimal weight = (BigDecimal) Base.firstCell("SELECT declared_weight FROM bag_reporting WHERE big_bag_id= ?", bbId); if (weight==null){weight= BigDecimal.valueOf(0);}
//            String lhname = (String) Base.firstCell("SELECT name FROM channels WHERE id IN (SELECT  channel_id FROM bag_reporting WHERE big_bag_id= ?)", bbId);
//            double handging = ((double) Math.round(weight.doubleValue()*handgilgRate*100))/100;
//            double linehaul = ((double) Math.round(weight.doubleValue()*linehaulFee*100))/100;
//            double gross = handging+linehaul;
////            Timestamp stamp = ((Timestamp)Base.firstCell("SELECT handover FROM bag_reporting WHERE big_bag_id= ?", bbId));
////            if (stamp != null){time = stamp.toLocalDateTime().atZone(ZoneId.of("UTC+0")).withZoneSameInstant(ZoneId.of("UTC+8")).toLocalDateTime();}
//            Object [] rowFill = {transportDocument, bbId,weight.doubleValue(),placeOfOrigin,departureIATACode,destinationIATACode,sellingProduct,freightAgent,lhname,time,gross,handgilgRate,handging,linehaulFee,linehaul};
//
//            Row row = seaSheet.createRow(rownumseas++);
//            int colNum = 0;
//            for (Object field : rowFill) {
//                Cell cell = row.createCell(colNum++);
//                if (field instanceof String) {
//                    cell.setCellValue((String) field);
//                } else if (field instanceof Integer) {
//                    cell.setCellValue((Integer) field);
//                } else if (field instanceof BigDecimal) {
//                    cell.setCellValue((RichTextString) field);
//                } else if (field instanceof Double) {
//                    cell.setCellValue((Double) field);
//                } else if (field instanceof Timestamp) {
//                    cell.setCellValue((Timestamp) field);
//                } else if (field instanceof LocalDateTime) {
//                    cell.setCellValue(String.valueOf((LocalDateTime) field));
//                }
//            }
//        };
    }

}
