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
    XSSFSheet sheetNoPickup = failedorders.createSheet("No Pickup");
    XSSFSheet sheetNoHoOut = failedorders.createSheet("No HoOut");

    XSSFFont font = failedorders.createFont();
    XSSFCellStyle style = failedorders.createCellStyle();

    int row_pfFb = 0;
    int row_pfNfb = 0;
    int row_pHOfFb = 0;
    int row_pHOfNfb = 0;
    int row_NoPickup = 0;
    int row_NoHoOut = 0;



    public static void main(String[] args) {
        SpringApplication.run(FailedOrdersCargoflow.class, args);
    }
    @Override
    public void run(String... args) throws Exception {
        List<Order>pickupfailedFullBag = new ArrayList<>();
        List<Order>pickupfailedNotFullBag = new ArrayList<>();
        List<Order>pickupHoOutfailedFullBag = new ArrayList<>();
        List<Order>pickupHoOutfailedNotFullBag = new ArrayList<>();
        List<Order> noPickup = new ArrayList<>();
        List<Order> noHoOut = new ArrayList<>();

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

        List<Order> orderpickupfailed =Order.findBySQL("select o.* from \"order\" o left outer join  task_queue tq  on tq.correlation_id =  o.logistics_order_code where tq.create_time > now() - interval '100 day' and tq.actor = 'CAINIAO_GLOBAL_PICKUP_CALLBACK' and tq.status = 'FAIL'");
        List<Order> orderpickupHooutfailed =Order.findBySQL("select o.* from \"order\" o left outer join  task_queue tq  on tq.correlation_id =  o.logistics_order_code where tq.create_time > now() - interval '100 day' and tq.actor = 'CAINIAO_GLOBAL_PICKUP_HOOUT_CALLBACK' and tq.status = 'FAIL'");


        List<Bag> bagpickup = Bag.findBySQL("select  * from bag  where big_bag_id in (select o.big_bag_id from \"order\" o left outer join  task_queue tq  on tq.correlation_id =  o.logistics_order_code where tq.create_time > now() - interval '100 day' and tq.actor = 'CAINIAO_GLOBAL_PICKUP_CALLBACK' and tq.status = 'FAIL')");
        List<Bag> bagpickupHoout = Bag.findBySQL("select  * from bag  where big_bag_id in (select o.big_bag_id from \"order\" o left outer join  task_queue tq  on tq.correlation_id =  o.logistics_order_code where tq.create_time > now() - interval '100 day' and tq.actor = 'CAINIAO_GLOBAL_PICKUP_HOOUT_CALLBACK' and tq.status = 'FAIL')");
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
        List<String> noPickupLOCs = Base.firstColumn("SELECT correlation_id from task_queue where create_time > now() - interval '10 day'  and queue_name='CAINIAO_STATUS_QUEUE' and actor = 'CAINIAO_GLOBAL_PICKUP_HOOUT_CALLBACK' and not EXISTS(SELECT actor from task_queue where actor = 'CAINIAO_GLOBAL_PICKUP_CALLBACK')");
        List<String> noHoOutLOCs = Base.firstColumn("SELECT correlation_id from task_queue where create_time > now() - interval '10 day'  and queue_name='CAINIAO_STATUS_QUEUE' and actor = 'CAINIAO_GLOBAL_PICKUP_CALLBACK' and not EXISTS(SELECT actor from task_queue where actor = 'CAINIAO_GLOBAL_PICKUP_HOOUT_CALLBACK')");

        for (String item: noPickupLOCs){noPickup.add((Order) Base.firstCell("find * from \"order\" o where o.logistics_order_code= ?",item));}
        for (String item: noHoOutLOCs){noHoOut.add((Order) Base.firstCell("find * from \"order\" o where o.logistics_order_code= ?",item));}



        System.out.println("total quantity of failed orders = "+orderpickupfailed.size()+" and "+orderpickupHooutfailed.size());
        System.out.println("quantity of pickupfailedFullBag = "+pickupfailedFullBag.size());
        System.out.println("quantity of pickupfailedNotFullBag = "+pickupfailedNotFullBag.size());
       System.out.println("quantity of pickupHoOutfailedFullBag = "+pickupHoOutfailedFullBag.size());
        System.out.println("quantity of pickupHoOutfailedNotFullBag = "+pickupHoOutfailedNotFullBag.size());

        fillingSheet(pickupfailedFullBag, sheetFullBag, row_pfFb, "CAINIAO_GLOBAL_PICKUP_CALLBACK");
        fillingSheet(pickupfailedNotFullBag,sheetNotFullBag,row_pfNfb, "CAINIAO_GLOBAL_PICKUP_CALLBACK");
        fillingSheet(pickupHoOutfailedFullBag,sheetHoOutFullBag,row_pHOfFb, "CAINIAO_GLOBAL_PICKUP_HOOUT_CALLBACK");
        fillingSheet(pickupHoOutfailedNotFullBag,sheetHoOutNotFullBag,row_pHOfNfb, "CAINIAO_GLOBAL_PICKUP_HOOUT_CALLBACK");


        sheetFullBag.setColumnWidth(0,5000);sheetFullBag.setColumnWidth(1,5000);sheetFullBag.setColumnWidth(2,4000);sheetFullBag.setColumnWidth(3,3000); sheetFullBag.setColumnWidth(4,5000);
        sheetFullBag.setColumnWidth(5,4200);sheetFullBag.setColumnWidth(6,3000);sheetFullBag.setColumnWidth(7,2500);sheetFullBag.setColumnWidth(8,4300);sheetFullBag.setColumnWidth(9,50000);

        sheetNotFullBag.setColumnWidth(0,5000);sheetNotFullBag.setColumnWidth(1,5000);sheetNotFullBag.setColumnWidth(2,4000);sheetNotFullBag.setColumnWidth(3,3000); sheetNotFullBag.setColumnWidth(4,5000);
        sheetNotFullBag.setColumnWidth(5,4200);sheetNotFullBag.setColumnWidth(6,3000);sheetNotFullBag.setColumnWidth(7,2500);sheetNotFullBag.setColumnWidth(8,4300);sheetNotFullBag.setColumnWidth(9,50000);

        sheetHoOutFullBag.setColumnWidth(0,5000);sheetHoOutFullBag.setColumnWidth(1,5000);sheetHoOutFullBag.setColumnWidth(2,4000);sheetHoOutFullBag.setColumnWidth(3,3000); sheetHoOutFullBag.setColumnWidth(4,5000);
        sheetHoOutFullBag.setColumnWidth(5,4200);sheetHoOutFullBag.setColumnWidth(6,3000);sheetHoOutFullBag.setColumnWidth(7,2500);sheetHoOutFullBag.setColumnWidth(8,4300);sheetHoOutFullBag.setColumnWidth(9,50000);

        sheetHoOutNotFullBag.setColumnWidth(0,5000);sheetHoOutNotFullBag.setColumnWidth(1,5000);sheetHoOutNotFullBag.setColumnWidth(2,4000);sheetHoOutNotFullBag.setColumnWidth(3,3000); sheetHoOutNotFullBag.setColumnWidth(4,5000);
        sheetHoOutNotFullBag.setColumnWidth(5,4200);sheetHoOutNotFullBag.setColumnWidth(6,3000);sheetHoOutNotFullBag.setColumnWidth(7,2500);sheetHoOutNotFullBag.setColumnWidth(8,4300);sheetHoOutNotFullBag.setColumnWidth(9,50000);

        sheetNoPickup.setColumnWidth(0,5000);sheetNoPickup.setColumnWidth(1,5000);sheetNoPickup.setColumnWidth(2,4000);sheetNoPickup.setColumnWidth(3,3000); sheetNoPickup.setColumnWidth(4,5000);
        sheetNoPickup.setColumnWidth(5,4200);sheetNoPickup.setColumnWidth(6,3000);sheetNoPickup.setColumnWidth(7,2500);sheetNoPickup.setColumnWidth(8,4300);sheetNoPickup.setColumnWidth(9,50000);

        sheetNoHoOut.setColumnWidth(0,5000);sheetNoHoOut.setColumnWidth(1,5000);sheetNoHoOut.setColumnWidth(2,4000);sheetNoHoOut.setColumnWidth(3,3000); sheetNoHoOut.setColumnWidth(4,5000);
        sheetNoHoOut.setColumnWidth(5,4200);sheetNoHoOut.setColumnWidth(6,3000);sheetNoHoOut.setColumnWidth(7,2500);sheetNoHoOut.setColumnWidth(8,4300);sheetNoHoOut.setColumnWidth(9,50000);



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
        Object[] datatype = {"logistics_order_code", "tracking_number","big_bag_id", "provider_id", "created_at", "reference_id", "lastmile_id","status_tq","errorCode", "task queue response"};

            Row row_Fb = sheetFullBag.createRow(row_pfFb++);
            Row row_nFb = sheetNotFullBag.createRow(row_pfNfb++);
            Row row_HOFb = sheetHoOutFullBag.createRow(row_pHOfFb++);
            Row row_HONfb = sheetHoOutNotFullBag.createRow(row_pHOfNfb++);
            Row row_NoPickUp = sheetNoPickup.createRow(row_NoPickup);
            Row row_NoHoout = sheetNoHoOut.createRow(row_NoHoOut);
            int colNum = 0;
            for (Object field : datatype) {
                Cell cell1 = row_Fb.createCell(colNum);
                Cell cell2 = row_nFb.createCell(colNum);
                Cell cell3 = row_HOFb.createCell(colNum);
                Cell cell4 = row_HONfb.createCell(colNum);
                Cell cell5 = row_NoPickUp.createCell(colNum);
                Cell cell6 = row_NoHoout.createCell(colNum);
                colNum++;
                if (field instanceof String) {
                    cell1.setCellValue((String) field);
                    cell2.setCellValue((String) field);
                    cell3.setCellValue((String) field);
                    cell4.setCellValue((String) field);
                    cell5.setCellValue((String) field);
                    cell6.setCellValue((String) field);
                }
            }
    }
    public void fillingSheet(List<Order> items, XSSFSheet sheet, int rowNo, String actor){

        for (Order item: items) {
            String loc = item.getLogisticsOrderCode();
            String trNum = item.getTrackingNumber();
            String bbId = item.getBigBagId();
            int provId = item.getProviderId();
            Timestamp creAt = item.getCreatedAt();
            String refId = item.getReferenceId();
            int lastmileId = item.getLastMileId();
            String statTq = (String) Base.firstCell("SELECT status FROM task_queue WHERE status='FAIL' and correlation_id= ? and actor= ?", item.getLogisticsOrderCode(),actor);
            String tqResponse = (String) Base.firstCell("SELECT response FROM task_queue WHERE status='FAIL' and correlation_id= ? and actor= ?", item.getLogisticsOrderCode(),actor);
            String errorCode = tqResponse.substring(32, 50);

            Object [] rowFill = {loc,trNum,bbId,provId,creAt,refId,lastmileId,statTq,errorCode,tqResponse};

            Row row = sheet.createRow(rowNo++);
            int colNum = 0;
            for (Object field : rowFill) {
                Cell cell = row.createCell(colNum++);
                if (field instanceof String) {
                    cell.setCellValue((String) field);
                } else if (field instanceof Integer) {
                    cell.setCellValue((Integer) field);
                }  else if (field instanceof Timestamp) {
                    cell.setCellValue(String.valueOf((Timestamp) field));
                }
            }
        };
    }
}
