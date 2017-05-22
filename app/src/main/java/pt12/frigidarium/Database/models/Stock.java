package pt12.frigidarium.Database.models;

import android.net.sip.SipAudioCall;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import pt12.frigidarium.Database.firebase.DatabaseEntry;
import pt12.frigidarium.Database.firebase.DatabaseEntryOwner;
import pt12.frigidarium.Database.firebase.DatabaseGroupedEntry;
import pt12.frigidarium.Database.firebase.DatabaseMapEntry;
import pt12.frigidarium.Database.firebase.DatabaseSingleEntry;

/**
 * Created by mattijn on 15/05/17.
 */

public class Stock extends DatabaseEntryOwner<Stock> {
    public static final String USERS = "users";
    public static final String INSTOCK =  "in_stock";
    public static final String OUTSTOCK = "out_stock";
    public static final String NAME = "name";
    private static Map<String,Stock> stocks= new HashMap<>();

    /**
     * Use this function to create a Stock. This Stock will be passed in callback.
     * @param uid the uid of a Stocl
     * @param callback the callback after The Stock has been created.
     */
    public static void getInstanceByUID(String uid, final DatabaseEntryOwner.onReadyCallback<Stock> callback){
        Stock s = getInstanceByUID(uid);
        final boolean[] called = {false};
        s.addDataAccessor(new DataAccessor<Stock>() {
            @Override
            public void onError(Stock owner, String name, int code, String message, String details) {
                callback.onError(owner,name,code,message,details);
            }

            @Override
            public void onGetInstance(Stock owner) {
                called[0] = true;
                if (getUid() == null || getUid().equals("")){
                    callback.OnDoesNotExist(owner);
                }else {
                    callback.onExist(owner);
                }
            }
        });
        if (!called[0] && s.isFinished()){
            for (final DataAccessor<Stock> l: stocks.get(uid).getDataAccessors()){
                l.onGetInstance(stocks.get(uid));
            }
        }
    }
    /**
     * Use this function to create a Stock.
     * @param uid the uid of a stock
     * @return null if the stock does not exsist in the database
     */
    public static Stock getInstanceByUID(final String uid){
        if (!stocks.containsKey(uid)){
            stocks.put(uid,new Stock(uid));
        }
        for (final DataAccessor<Stock> l: stocks.get(uid).getDataAccessors()){
            DatabaseEntryOwner.OnFinishedListener<Stock>  lf = new OnFinishedListener<Stock>() {
                @Override
                public void onFinished(Stock owner) {
                    l.onGetInstance(stocks.get(uid));
                }
            };
            stocks.get(uid).addOnFinishedListener(lf);
        }
        return stocks.get(uid);
    }

    /**
     * This function creates a new entry in the firebase database.
     * However if the User already exists it will be overridden.
     * @param uid the firebaseuid of the user.
     * @param name the name of the user
     * @return the newly created entry
     */
    public static Stock createUser(String uid, String name){
        Stock s =  Stock.getInstanceByUID(uid);
        ((DatabaseSingleEntry<Product,String>)  s.getEntry(UID)).setValue(uid);
        ((DatabaseSingleEntry<Product,String>)  s.getEntry(NAME)).setValue(name);
        return s;
    }

    private static DatabaseReference createReference(String uid){
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("stocks").child(uid);
        return myRef;
    }
    private static Map<String, DatabaseEntry> getEntries(String uid){
        DatabaseReference ref = createReference(uid);
        Map<String, DatabaseEntry>  entries = new HashMap<>();
        entries.put(NAME, new DatabaseSingleEntry<Stock,String>(NAME, ref.child(NAME), String.class));
        entries.put(USERS, new DatabaseMapEntry<Stock,String>(USERS, ref.child(USERS), String.class));
        entries.put(INSTOCK, new DatabaseGroupedEntry<Stock,StockEntry>(INSTOCK, ref.child(INSTOCK), StockEntry.class));
        entries.put(OUTSTOCK, new DatabaseGroupedEntry<Stock,StockEntry>(OUTSTOCK, ref.child(OUTSTOCK), StockEntry.class));
        return entries;
    }

    private Stock(String uid){
        super(uid, createReference(uid),getEntries(uid));
        DatabaseGroupedEntry instock = (DatabaseGroupedEntry) getEntries(INSTOCK);
        DatabaseGroupedEntry outstock= (DatabaseGroupedEntry) getEntries(INSTOCK);
        DatabaseMapEntry<Stock,String> users= (DatabaseMapEntry) getEntries(USERS);
        instock.addListener(new DatabaseGroupedEntry.OnChangeListener<Stock, StockEntry>() {
            @Override
            public void onGroupAdded(Stock owner, DatabaseGroupedEntry<Stock, StockEntry> value, DatabaseMapEntry<Stock, StockEntry> group) {
                for (DatabaseEntryOwner.DataAccessor<Stock> listener : getDataAccessors()){
                    if (listener instanceof Stock.OnStockTypeChanged){
                        OnStockTypeChanged l = (OnStockTypeChanged) listener;
                        l.onProductTypeAddedToInStock(owner, Product.getInstanceByUID(group.getName()));
                    }
                }
            }

            @Override
            public void onGroupRemoved(Stock owner, DatabaseGroupedEntry<Stock, StockEntry> value, DatabaseMapEntry<Stock, StockEntry> group) {
                for (DatabaseEntryOwner.DataAccessor<Stock> listener : getDataAccessors()){
                    if (listener instanceof Stock.OnStockTypeChanged){
                        OnStockTypeChanged l = (OnStockTypeChanged) listener;
                        l.onProductTypeRemovedToInStock(owner, Product.getInstanceByUID(group.getName()));
                    }
                }
            }

            @Override
            public void onEntryAdded(Stock owner, DatabaseGroupedEntry<Stock, StockEntry> grouped, DatabaseMapEntry<Stock, StockEntry> group, StockEntry entry, String groupUid, String key) {
                for (DatabaseEntryOwner.DataAccessor<Stock> listener : getDataAccessors()){
                    if (listener instanceof Stock.OnStockInstanceChanged){
                        OnStockInstanceChanged l = (OnStockInstanceChanged) listener;
                        l.onProductInstanceAddedToInStock(owner,
                                Product.getInstanceByUID(groupUid),
                                key,entry
                                );
                    }
                }
            }

            @Override
            public void onEntryChanged(Stock owner, DatabaseGroupedEntry<Stock, StockEntry> t, DatabaseMapEntry<Stock, StockEntry> group, StockEntry oldElement, StockEntry element, String groupUid, String key) {

            }

            @Override
            public void onEntryRemoved(Stock owner, DatabaseGroupedEntry<Stock, StockEntry> t, DatabaseMapEntry<Stock, StockEntry> group, StockEntry entry, String groupUid, String key) {
                for (DatabaseEntryOwner.DataAccessor<Stock> listener : getDataAccessors()){
                    if (listener instanceof Stock.OnStockInstanceChanged){
                        OnStockInstanceChanged l = (OnStockInstanceChanged) listener;
                        l.onProductInstanceRemovedToInStock(owner,
                                Product.getInstanceByUID(groupUid),
                                key,entry
                        );
                    }
                }
            }

            @Override
            public void onError(Stock owner, String name, int code, String message, String details) {
                for (DatabaseEntryOwner.DataAccessor<Stock> listener : getDataAccessors()){
                    listener.onError(owner, name, code, message,details);
                }
            }
        });


        outstock.addListener(new DatabaseGroupedEntry.OnChangeListener<Stock, StockEntry>() {
            @Override
            public void onGroupAdded(Stock owner, DatabaseGroupedEntry<Stock, StockEntry> value, DatabaseMapEntry<Stock, StockEntry> group) {
                for (DatabaseEntryOwner.DataAccessor<Stock> listener : getDataAccessors()){
                    if (listener instanceof Stock.OnStockTypeChanged){
                        OnStockTypeChanged l = (OnStockTypeChanged) listener;
                        l.onProductTypeAddedToOutStock(owner, Product.getInstanceByUID(group.getName()));
                    }
                }
            }

            @Override
            public void onGroupRemoved(Stock owner, DatabaseGroupedEntry<Stock, StockEntry> value, DatabaseMapEntry<Stock, StockEntry> group) {
                for (DatabaseEntryOwner.DataAccessor<Stock> listener : getDataAccessors()){
                    if (listener instanceof Stock.OnStockTypeChanged){
                        OnStockTypeChanged l = (OnStockTypeChanged) listener;
                        l.onProductTypeRemovedToOutStock(owner, Product.getInstanceByUID(group.getName()));
                    }
                }
            }

            @Override
            public void onEntryAdded(Stock owner, DatabaseGroupedEntry<Stock, StockEntry> grouped, DatabaseMapEntry<Stock, StockEntry> group, StockEntry entry, String groupUid, String key) {
                for (DatabaseEntryOwner.DataAccessor<Stock> listener : getDataAccessors()){
                    if (listener instanceof Stock.OnStockInstanceChanged){
                        OnStockInstanceChanged l = (OnStockInstanceChanged) listener;
                        l.onProductInstanceAddedToOutStock(owner,
                                Product.getInstanceByUID(groupUid),
                                key,entry
                        );
                    }
                }
            }

            @Override
            public void onEntryChanged(Stock owner, DatabaseGroupedEntry<Stock, StockEntry> t, DatabaseMapEntry<Stock, StockEntry> group, StockEntry oldElement, StockEntry element, String groupUid, String key) {

            }

            @Override
            public void onEntryRemoved(Stock owner, DatabaseGroupedEntry<Stock, StockEntry> t, DatabaseMapEntry<Stock, StockEntry> group, StockEntry entry, String groupUid, String key) {
                for (DatabaseEntryOwner.DataAccessor<Stock> listener : getDataAccessors()){
                    if (listener instanceof Stock.OnStockInstanceChanged){
                        OnStockInstanceChanged l = (OnStockInstanceChanged) listener;
                        l.onProductInstanceRemovedToOutStock(owner,
                                Product.getInstanceByUID(groupUid),
                                key,entry
                        );
                    }
                }
            }

            @Override
            public void onError(Stock owner, String name, int code, String message, String details) {
                for (DatabaseEntryOwner.DataAccessor<Stock> listener : getDataAccessors()){
                    listener.onError(owner, name, code, message,details);
                }
            }
        });
        users.addListener(new DatabaseMapEntry.OnChangeListener<Stock,String>() {
            @Override
            public void onChildAdded(Stock owner, String mapName, String element, String key) {
                for (DatabaseEntryOwner.DataAccessor<Stock> listener : getDataAccessors()){
                    if (listener instanceof Stock.OnStockUserChanged){
                        OnStockUserChanged l = (OnStockUserChanged) listener;
                        l.onUserAdded(owner, User.getInstanceByUID(element));
                    }
                }
            }

            @Override
            public void onChildChanged(Stock owner, String mapName, String element, String key, String oldElement) {
            }

            @Override
            public void onChildRemoved(Stock owner, String mapName, String element, String key) {
                for (DatabaseEntryOwner.DataAccessor<Stock> listener : getDataAccessors()){
                    if (listener instanceof Stock.OnStockUserChanged){
                        OnStockUserChanged l = (OnStockUserChanged) listener;
                        l.onUserRemoved(owner, User.getInstanceByUID(element));
                    }
                }
            }

            @Override
            public void onError(Stock owner, String name, int code, String message, String details) {
                for (DatabaseEntryOwner.DataAccessor<Stock> listener : getDataAccessors()){
                    listener.onError(owner, name, code, message,details);
                }
            }
        });

    }
    public void removeFromOutOfStock(StockEntry entry){
        DatabaseGroupedEntry<Stock, StockEntry> outofstock  = (DatabaseGroupedEntry<Stock, StockEntry>) getEntry(OUTSTOCK);
        outofstock.removeEntry(entry,entry.getProduct_uid());
    }
    public void addToOutOfStock(StockEntry entry){
        DatabaseGroupedEntry<Stock, StockEntry> outofstock  = (DatabaseGroupedEntry<Stock, StockEntry>) getEntry(OUTSTOCK);
        outofstock.addEntry(entry, entry.getProduct_uid());
    }

    public void moveInStock(StockEntry entry){
        DatabaseGroupedEntry<Stock, StockEntry> outofstock  = (DatabaseGroupedEntry<Stock, StockEntry>) getEntry(OUTSTOCK);
        DatabaseGroupedEntry<Stock, StockEntry> instock  = (DatabaseGroupedEntry<Stock, StockEntry>) getEntry(INSTOCK);
        outofstock.removeEntry(entry,entry.getProduct_uid());
        instock.addEntry(entry, entry.getProduct_uid());
    }

    public void moveOutOfStock(StockEntry entry){
        DatabaseGroupedEntry<Stock, StockEntry> outofstock  = (DatabaseGroupedEntry<Stock, StockEntry>) getEntry(OUTSTOCK);
        DatabaseGroupedEntry<Stock, StockEntry> instock  = (DatabaseGroupedEntry<Stock, StockEntry>) getEntry(INSTOCK);
        instock.removeEntry(entry,entry.getProduct_uid());
        outofstock.addEntry(entry, entry.getProduct_uid());
    }

    public static class StockEvent{
        Map<String, String> timestamp;
        String user_uid;
        String product_uid;
        String event;
        Double amount;

        public StockEvent() {}

        public Map<String, String> getTimestamp(){
            return timestamp;
        }
        public String getUser_uid(){
            return user_uid;
        }
        public String getProduct_uid(){
            return product_uid;
        }
        public String getEvent(){
            return event;
        }
        public Double getAmount(){
            return amount;
        }
    }

    public static class StockEntry {
        String product_uid;
        Map<String, String> timeAdded;
        Long best_before;
        String addedByUser;

        /**
         * never call this constructor. use the other one;
         *
         */
        @Deprecated
        public StockEntry() {
        }

        public StockEntry(String product_uid, Long best_before) {
            this.product_uid = product_uid;
            this.best_before = best_before;
            addedByUser = FirebaseAuth.getInstance().getCurrentUser().getUid();
            timeAdded= ServerValue.TIMESTAMP;
        }

        public String getProduct_uid() {
            return product_uid;
        }

        public String toString() {
            return "{" + product_uid + ":" + best_before + "}";
        }

        /**
         * looks at the product_uid and the bestbefore date.
         * @param o
         * @return
         */
        @Override
        public boolean equals(Object o) {
            if (o instanceof StockEntry) {
                if (best_before == null && ((StockEntry) o).best_before == null) {
                    return product_uid.equals(((StockEntry) o).getProduct_uid());
                }else if (best_before == null && !(((StockEntry) o).best_before == null)) {
                    return false;
                }else{
                    return product_uid.equals(((StockEntry) o).getProduct_uid())  && best_before.equals(((StockEntry) o).best_before);
                }
            }
            return true;
        }
    }

    public abstract static class OnStockChangedListener extends DataAccessor<Stock>{

        protected Set<Product> getProductsInStock(){
            Set<String> products_uids =  ((DatabaseGroupedEntry<Stock,StockEntry>) getOwner().getEntry(INSTOCK)).getGroups();
            Set<Product>  products = new HashSet<>();
            for (String uid : products_uids){
                products.add(Product.getInstanceByUID(uid));
            }
            return products;
        }

        protected Set<Product> getProductsOutStock(){
            Set<String> products_uids =  ((DatabaseGroupedEntry<Stock,StockEntry>) getOwner().getEntry(OUTSTOCK)).getGroups();
            Set<Product>  products = new HashSet<>();
            for (String uid : products_uids){
                products.add(Product.getInstanceByUID(uid));
            }
            return products;
        }
        protected String getName(){
            return ((DatabaseSingleEntry<Stock,String>)  getOwner().getEntry(NAME)).getValue();
        }

        protected Map<String,User> getUsers(){
            Map<String,String> map  = ((DatabaseMapEntry<Stock,String>)  getOwner().getEntry(USERS)).getMap();
            Map<String, User>  users  =  new HashMap<>();
            for (Map.Entry<String,String> entry: map.entrySet()){
                users.put(entry.getKey(),User.getInstanceByUID(entry.getValue()));
            }
            return users;
        }
    }

    public abstract static class OnStockUserChanged extends OnStockChangedListener{
        public abstract void onUserAdded(Stock s, User u);
        public abstract void onUserRemoved(Stock s, User u);
    }

    public abstract static class OnStockTypeChanged extends OnStockChangedListener{
        public abstract void onProductTypeAddedToInStock(Stock s, Product p);
        public abstract void onProductTypeRemovedToInStock(Stock s, Product p);
        public abstract void onProductTypeRemovedToOutStock(Stock s, Product p);
        public abstract void onProductTypeAddedToOutStock(Stock s, Product p);
    }

    public abstract static class OnStockInstanceChanged extends OnStockChangedListener{
        public abstract void onProductInstanceAddedToInStock(Stock s, Product p, String key, StockEntry instance);
        public abstract void onProductInstanceRemovedToInStock(Stock s, Product p, String key, StockEntry instance);
        public abstract void onProductInstanceRemovedToOutStock(Stock s, Product p, String key, StockEntry instance);
        public abstract void onProductInstanceAddedToOutStock(Stock s, Product p, String key, StockEntry instance);
    }
}