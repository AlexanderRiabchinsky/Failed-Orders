package app.cargoflow.Model;

import org.javalite.activejdbc.*;
import org.javalite.activejdbc.annotations.Cached;
import org.javalite.activejdbc.annotations.Table;
import org.javalite.activejdbc.validation.Converter;
import org.javalite.activejdbc.validation.NumericValidationBuilder;
import org.javalite.activejdbc.validation.ValidationBuilder;
import org.javalite.activejdbc.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import java.text.DateFormat;
import java.util.List;
import java.util.Set;

@Table("order")
public class Order extends Model{
    public static <T extends Model> LazyList<T> findBySQL(String fullQuery, Object... params) {
        return ModelDelegate.findBySql(modelClass(), fullQuery, params);
    }
    public static MetaModel getMetaModel() {
        return ModelDelegate.metaModelOf(modelClass());
    }

    public static MetaModel metaModel() {
        return ModelDelegate.metaModelOf(modelClass());
    }

    public static <T extends Model> T findOrCreateIt(Object... namesAndValues) {
        return ModelDelegate.findOrCreateIt(modelClass(), namesAndValues);
    }

    public static <T extends Model> T findOrInit(Object... namesAndValues) {
        return ModelDelegate.findOrInit(modelClass(), namesAndValues);
    }

    public static List<String> attributes() {
        return ModelDelegate.attributes(modelClass());
    }

    public static Set<String> attributeNames() {
        return ModelDelegate.attributeNames(modelClass());
    }

    public static List<Association> associations() {
        return ModelDelegate.associations(modelClass());
    }

    public static int delete(String query, Object... params) {
        return ModelDelegate.delete(modelClass(), query, params);
    }

    public static boolean exists(Object id) {
        return ModelDelegate.exists(modelClass(), id);
    }

    public static int deleteAll() {
        return ModelDelegate.deleteAll(modelClass());
    }

    public static int update(String updates, String conditions, Object... params) {
        return ModelDelegate.update(modelClass(), updates, conditions, params);
    }

    public static int updateAll(String updates, Object... params) {
        return ModelDelegate.updateAll(modelClass(), updates, params);
    }

    protected static NumericValidationBuilder validateNumericalityOf(String... attributeNames) {
        return ModelDelegate.validateNumericalityOf(modelClass(), attributeNames);
    }

    public static ValidationBuilder addValidator(Validator validator) {
        return ModelDelegate.validateWith(modelClass(), validator);
    }

    protected static void addScope(String name, String criteria) {
        ModelDelegate.addScope(modelClass().getName(), name, criteria);
    }

    public static void removeValidator(Validator validator) {
        ModelDelegate.removeValidator(modelClass(), validator);
    }

    public static List<Validator> getValidators(Class<? extends Model> clazz) {
        return ModelDelegate.validatorsOf(clazz);
    }

    protected static ValidationBuilder validateRegexpOf(String attributeName, String pattern) {
        return ModelDelegate.validateRegexpOf(modelClass(), attributeName, pattern);
    }

    protected static ValidationBuilder validateEmailOf(String attributeName) {
        return ModelDelegate.validateEmailOf(modelClass(), attributeName);
    }

    protected static ValidationBuilder validateRange(String attributeName, Number min, Number max) {
        return ModelDelegate.validateRange(modelClass(), attributeName, min, max);
    }

    protected static ValidationBuilder validatePresenceOf(String... attributeNames) {
        return ModelDelegate.validatePresenceOf(modelClass(), attributeNames);
    }

    protected static ValidationBuilder validateWith(Validator validator) {
        return ModelDelegate.validateWith(modelClass(), validator);
    }

//    protected static ValidationBuilder convertWith(Converter converter) {
//        return ModelDelegate.convertWith(modelClass(), converter);
//    }

    protected static void convertWith(org.javalite.activejdbc.conversion.Converter converter, String... attributeNames) {
        ModelDelegate.convertWith(modelClass(), converter, attributeNames);
    }

    protected static ValidationBuilder convertDate(String attributeName, String format) {
        return ModelDelegate.convertDate(modelClass(), attributeName, format);
    }

    protected static ValidationBuilder convertTimestamp(String attributeName, String format) {
        return ModelDelegate.convertTimestamp(modelClass(), attributeName, format);
    }

    protected static void dateFormat(String pattern, String... attributeNames) {
        ModelDelegate.dateFormat(modelClass(), pattern, attributeNames);
    }

    protected static void dateFormat(DateFormat format, String... attributeNames) {
        ModelDelegate.dateFormat(modelClass(), format, attributeNames);
    }

    protected static void timestampFormat(String pattern, String... attributeNames) {
        ModelDelegate.timestampFormat(modelClass(), pattern, attributeNames);
    }

    protected static void timestampFormat(DateFormat format, String... attributeNames) {
        ModelDelegate.timestampFormat(modelClass(), format, attributeNames);
    }

    protected static void blankToNull(String... attributeNames) {
        ModelDelegate.blankToNull(modelClass(), attributeNames);
    }

    protected static void zeroToNull(String... attributeNames) {
        ModelDelegate.zeroToNull(modelClass(), attributeNames);
    }

    public static boolean belongsTo(Class<? extends Model> targetClass) {
        return ModelDelegate.belongsTo(modelClass(), targetClass);
    }

    public static void addCallbacks(CallbackListener... listeners) {
        ModelDelegate.callbackWith(modelClass(), listeners);
    }

    public static void callbackWith(CallbackListener... listeners) {
        ModelDelegate.callbackWith(modelClass(), listeners);
    }

    public static <T extends Model> T create(Object... namesAndValues) {
        return ModelDelegate.create(modelClass(), namesAndValues);
    }

    public static <T extends Model> T createIt(Object... namesAndValues) {
        return ModelDelegate.createIt(modelClass(), namesAndValues);
    }

    public static <T extends Model> T findById(Object id) {
        return ModelDelegate.findById(modelClass(), id);
    }

    public static <T extends Model> T findByCompositeKeys(Object... values) {
        return ModelDelegate.findByCompositeKeys(modelClass(), values);
    }

    public static <T extends Model> LazyList<T> where(String subquery, Object... params) {
        return ModelDelegate.where(modelClass(), subquery, params);
    }

    public static <T extends Model> ScopeBuilder<T> scopes(String... scopes) {
        return new ScopeBuilder(modelClass(), scopes);
    }

    public static <T extends Model> ScopeBuilder<T> scope(String scope) {
        return new ScopeBuilder(modelClass(), new String[]{scope});
    }

    public static <T extends Model> LazyList<T> find(String subquery, Object... params) {
        return ModelDelegate.where(modelClass(), subquery, params);
    }

//    public static <T extends Model> T findFirst(String subQuery, Object... params) {
//        return ModelDelegate.findFirst(modelClass(), subQuery, params);
//    }

    public static <T extends Model> T first(String subQuery, Object... params) {
        return ModelDelegate.findFirst(modelClass(), subQuery, params);
    }

    public static void find(String query, ModelListener listener) {
        ModelDelegate.findWith(modelClass(), listener, query, new Object[0]);
    }

    public static void findWith(ModelListener listener, String query, Object... params) {
        ModelDelegate.findWith(modelClass(), listener, query, params);
    }

    public static <T extends Model> LazyList<T> findAll() {
        return ModelDelegate.findAll(modelClass());
    }

    public static Long count() {
        return ModelDelegate.count(modelClass());
    }

    public static Long count(String query, Object... params) {
        return ModelDelegate.count(modelClass(), query, params);
    }

    private static <T extends Model> Class<T> modelClass() {
        return (Class<T>) Order.class;
    }

    public static String getTableName() {
        return ModelDelegate.tableNameOf(modelClass());
    }

    public static boolean isCached() {
        return modelClass().getAnnotation(Cached.class) != null;
    }

    public static void purgeCache() {
        ModelDelegate.purgeCache(modelClass());
    }
    public String getLogisticsOrderCode() {
    return getString("logistics_order_code");
}
    public String getBigBagId() {
        return getString("big_bag_id");
    }
}
