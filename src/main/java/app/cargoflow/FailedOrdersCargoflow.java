package app.cargoflow;

import app.cargoflow.Model.Bag;
import app.cargoflow.Model.Order;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.javalite.activejdbc.Base;
import org.javalite.activejdbc.Configuration;
import org.javalite.activejdbc.DBException;
import org.javalite.activejdbc.Registry;
import org.javalite.activejdbc.connection_config.ConnectionJdbcSpec;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

@SpringBootApplication
public class FailedOrdersCargoflow implements CommandLineRunner {

    private static final Logger log = getLogger(FailedOrdersCargoflow.class);
    private static final String FILE_NAME = "D:/Failed Orders Results/failed_orders.xlsx";
    private static final int days = 110;


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
        List<Order> pickupfailedFullBag = new ArrayList<>();
        List<Order> pickupfailedNotFullBag = new ArrayList<>();
        List<Order> pickupHoOutfailedFullBag = new ArrayList<>();
        List<Order> pickupHoOutfailedNotFullBag = new ArrayList<>();
        List<Map> noPickup = new ArrayList<>();
        List<Map> noHoOut = new ArrayList<>();

        font.setBold(false);
        style.setBorderTop(BorderStyle.NONE);
        style.setFont(font);

        headers();

        log.info("Started");
        Base.open();
        log.info("Connected");

        Configuration config = Registry.instance().getConfiguration();
        ConnectionJdbcSpec spec = (ConnectionJdbcSpec) config.getCurrentConnectionSpec();
        if (spec == null) {
            throw new DBException("Could not find configuration in a property file for environment: " + config.getEnvironment() +
                    ". Are you sure you have a database.properties file configured?");
        }

        log.info("Starting to synchronize shipments with reported copies...");

        List<Order> orderpickupfailed = Order.findBySQL("select o.* from \"order\" o left outer join  task_queue tq  on tq.correlation_id =  o.logistics_order_code where tq.create_time > now() - interval '"
                +days+" day' and tq.actor = 'CAINIAO_GLOBAL_PICKUP_CALLBACK' and tq.status = 'FAIL'");
        List<Order> orderpickupHooutfailed = Order.findBySQL("select o.* from \"order\" o left outer join  task_queue tq  on tq.correlation_id =  o.logistics_order_code where tq.create_time > now() - interval '"
                +days+" day' and tq.actor = 'CAINIAO_GLOBAL_PICKUP_HOOUT_CALLBACK' and tq.status = 'FAIL'");


        List<Bag> bagpickup = Bag.findBySQL("select  * from bag  where big_bag_id in (select o.big_bag_id from \"order\" o left outer join  task_queue tq  on tq.correlation_id =  o.logistics_order_code where tq.create_time > now() - interval '"
                +days+" day' and tq.actor = 'CAINIAO_GLOBAL_PICKUP_CALLBACK' and tq.status = 'FAIL')");
        List<Bag> bagpickupHoout = Bag.findBySQL("select  * from bag  where big_bag_id in (select o.big_bag_id from \"order\" o left outer join  task_queue tq  on tq.correlation_id =  o.logistics_order_code where tq.create_time > now() - interval '"
                +days+" day' and tq.actor = 'CAINIAO_GLOBAL_PICKUP_HOOUT_CALLBACK' and tq.status = 'FAIL')");
        System.out.println("\n pickup failed orders processing started...");
        for (Bag item : bagpickup) {
            List<Order> list = new ArrayList<>();
            for (Order o : orderpickupfailed)
                if (item.getBigBagId().equals(o.getBigBagId())) {
                    list.add(o);
                }
            if (item.getOrderNumber() == list.size()) {
                pickupfailedFullBag.addAll(list);
            } else {
                pickupfailedNotFullBag.addAll(list);
            }
        }
        System.out.println("\n pickup hoout failed orders processing started...");
        for (Bag item : bagpickupHoout) {
            List<Order> list = new ArrayList<>();
            for (Order o : orderpickupHooutfailed)
                if (item.getBigBagId().equals(o.getBigBagId())) {
                    list.add(o);
                }
            if (item.getOrderNumber() == list.size()) {
                pickupHoOutfailedFullBag.addAll(list);
            } else {
                pickupHoOutfailedNotFullBag.addAll(list);
            }
        }
        List<String> noPickupLOCs = Base.firstColumn("SELECT correlation_id from task_queue extra where create_time > now() - interval '" +days+" day'  " +
                "and queue_name='CAINIAO_STATUS_QUEUE' and actor = 'CAINIAO_GLOBAL_PICKUP_HOOUT_CALLBACK' and not EXISTS(SELECT * from task_queue ins where actor = 'CAINIAO_GLOBAL_PICKUP_CALLBACK' and extra.correlation_id=ins.correlation_id)");
        List<String> noHoOutLOCs = Base.firstColumn("SELECT correlation_id from task_queue extra where create_time > now() - interval '"+days+" day'  and create_time < now() - interval '3 day' " +
                "and queue_name='CAINIAO_STATUS_QUEUE' and actor = 'CAINIAO_GLOBAL_PICKUP_CALLBACK' and not EXISTS(SELECT * from task_queue ins where actor = 'CAINIAO_GLOBAL_PICKUP_HOOUT_CALLBACK' and extra.correlation_id=ins.correlation_id)");

        for (String item : noPickupLOCs) {
            noPickup.add(Base.findAll("select o.* from \"order\" o where o.logistics_order_code= ?", item).get(0));
        }
        for (String item : noHoOutLOCs) {
            noHoOut.add(Base.findAll("select o.* from \"order\" o where o.logistics_order_code= ?", item).get(0));
        }


        System.out.println("\nquantity of pickupfailedFullBag = " + pickupfailedFullBag.size());
        System.out.println("quantity of pickupfailedNotFullBag = " + pickupfailedNotFullBag.size());
        System.out.println("quantity of pickupHoOutfailedFullBag = " + pickupHoOutfailedFullBag.size());
        System.out.println("quantity of pickupHoOutfailedNotFullBag = " + pickupHoOutfailedNotFullBag.size());
        System.out.println("quantity of no Pickup = " + noPickup.size());
        System.out.println("quantity of no HoOut = " + noHoOut.size());

        fillingSheet(pickupfailedFullBag, sheetFullBag, row_pfFb, "CAINIAO_GLOBAL_PICKUP_CALLBACK");
        fillingSheet(pickupfailedNotFullBag, sheetNotFullBag, row_pfNfb, "CAINIAO_GLOBAL_PICKUP_CALLBACK");
        fillingSheet(pickupHoOutfailedFullBag, sheetHoOutFullBag, row_pHOfFb, "CAINIAO_GLOBAL_PICKUP_HOOUT_CALLBACK");
        fillingSheet(pickupHoOutfailedNotFullBag, sheetHoOutNotFullBag, row_pHOfNfb, "CAINIAO_GLOBAL_PICKUP_HOOUT_CALLBACK");
        fillingMAPSheet(noPickup, sheetNoPickup, row_NoPickup, "CAINIAO_GLOBAL_PICKUP_HOOUT_CALLBACK");
        fillingMAPSheet(noHoOut, sheetNoHoOut, row_NoHoOut, "CAINIAO_GLOBAL_PICKUP_CALLBACK");


        int[]width ={5000,4900,4000,2900,5000,4200,2900,2500,4300,50000};
        for (int i=0;i<10;i++){sheetFullBag.setColumnWidth(i, width[i]);sheetNotFullBag.setColumnWidth(i, width[i]);sheetHoOutFullBag.setColumnWidth(i, width[i]);
            sheetHoOutNotFullBag.setColumnWidth(i, width[i]);sheetNoPickup.setColumnWidth(i, width[i]);sheetNoHoOut.setColumnWidth(i, width[i]);}

        try {
            FileOutputStream outputStream = new FileOutputStream(FILE_NAME);
            failedorders.write(outputStream);
            failedorders.close();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Base.close(true);
    }

    public void headers() {
        Object[] datatype = {"logistics_order_code", "tracking_number", "big_bag_id", "provider_id", "created_at", "reference_id", "lastmile_id", "status_tq", "errorCode", "task queue response"};

        Row row_Fb = sheetFullBag.createRow(row_pfFb++);
        Row row_nFb = sheetNotFullBag.createRow(row_pfNfb++);
        Row row_HOFb = sheetHoOutFullBag.createRow(row_pHOfFb++);
        Row row_HONfb = sheetHoOutNotFullBag.createRow(row_pHOfNfb++);
        Row row_NoPickUp = sheetNoPickup.createRow(row_NoPickup++);
        Row row_NoHoout = sheetNoHoOut.createRow(row_NoHoOut++);
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

    public void fillingSheet(@NotNull List<Order> items, XSSFSheet sheet, int rowNo, String actor) {

        for (Order item : items) {
            String loc = item.getLogisticsOrderCode();
            String trNum = item.getTrackingNumber();
            String bbId = item.getBigBagId();
            int provId = item.getProviderId();
            Timestamp creAt = item.getCreatedAt();
            String refId = item.getReferenceId();
            int lastmileId = item.getLastMileId();
            String statTq = (String) Base.firstCell("SELECT status FROM task_queue WHERE  correlation_id= ? and actor= ?", item.getLogisticsOrderCode(), actor);
            String tqResponse = (String) Base.firstCell("SELECT response FROM task_queue WHERE correlation_id= ? and actor= ?", item.getLogisticsOrderCode(), actor);
            String errorCode = tqResponse.substring(32, 50);

            Object[] rowFill = {loc, trNum, bbId, provId, creAt, refId, lastmileId, statTq, errorCode, tqResponse};

            Row row = sheet.createRow(rowNo++);

            int colNum = 0;
            for (Object field : rowFill) {
                Cell cell = row.createCell(colNum++);
                if (field instanceof String) {
                    cell.setCellValue((String) field);
                } else if (field instanceof Integer) {
                    cell.setCellValue((Integer) field);
                } else if (field instanceof Timestamp) {
                    cell.setCellValue(String.valueOf((Timestamp) field));
                }
            }
        }
    }

    public void fillingMAPSheet(@NotNull List<Map> items, XSSFSheet sheet, int rowNo, String actor) {

        for (Map item : items) {
            String loc = (String) item.get("logistics_order_code");
            String trNum = (String) item.get("tracking_number");
            String bbId = (String) item.get("big_bag_id");
            int provId = (int) item.get("provider_id");
            Timestamp creAt = (Timestamp) item.get("created_at");
            String refId = (String) item.get("reference_id");
            int lastmileId = (int) item.get("lastmile_id");
            String statTq = (String) Base.firstCell("SELECT status FROM task_queue WHERE  correlation_id= ? and actor= ?", loc, actor);
            String tqResponse = (String) Base.firstCell("SELECT response FROM task_queue WHERE correlation_id= ? and actor= ?", loc, actor);
            String errorCode = (tqResponse.length()>50) ? tqResponse.substring(32, 50) : "";

            Object[] rowFill = {loc, trNum, bbId, provId, creAt, refId, lastmileId, statTq, errorCode, tqResponse};

            Row row = sheet.createRow(rowNo++);

            int colNum = 0;
            for (Object field : rowFill) {
                Cell cell = row.createCell(colNum++);
                if (field instanceof String) {
                    cell.setCellValue((String) field);
                } else if (field instanceof Integer) {
                    cell.setCellValue((Integer) field);
                } else if (field instanceof Timestamp) {
                    cell.setCellValue(String.valueOf((Timestamp) field));
                }
            }
        }
    }
}
