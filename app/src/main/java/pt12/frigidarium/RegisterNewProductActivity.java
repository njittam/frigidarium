package pt12.frigidarium;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.robin.anus.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import pt12.frigidarium.database2.models.Product;
import pt12.frigidarium.database2.models.Stock;
import pt12.frigidarium.database2.models.StockEntry;

public class RegisterNewProductActivity extends AppCompatActivity {
    public static final String BARCODE = "barcode";
    public static final String EXDATE = "exdate";
    private EditText productName;
    private EditText productBrand;
    private EditText productContent;
    private Button submit;
    long exdate; //expiring date of product
    private Spinner contentUnitDropdown;
    private String barcode = "ERROR";

    /**
     * Set up form behaviour on creation of activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getIntent().getStringExtra(BARCODE) != null)
        {
            barcode = getIntent().getStringExtra(BARCODE);
        }
        getIntent().getLongExtra(EXDATE, exdate);
        setContentView(R.layout.fragment_register_new_product);
        productName = (EditText) findViewById(R.id.product_name);
        productBrand = (EditText) findViewById(R.id.product_brand);
        productContent = (EditText) findViewById(R.id.product_content);
        submit = (Button) findViewById(R.id.submit);
        contentUnitDropdown = (Spinner) findViewById(R.id.content_units_drop);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.content_units, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        contentUnitDropdown.setAdapter(adapter);
        contentUnitDropdown.setSelection(0);

        submit.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String pn, pb, pc, purl = "ditiseenurl";
                        pn = productName.getText().toString().trim();
                        pb = productBrand.getText().toString().trim();
                        pc = productContent.getText().toString().trim() + " " + contentUnitDropdown.getSelectedItem().toString();
                        if(pn.equals("") || pb.equals("") || productContent.getText().toString().trim().equals(""))
                        {
                            Toast.makeText(getApplicationContext(), R.string.not_all_field_filled_in, Toast.LENGTH_LONG).show();
                        }
                        else
                        {
                            RegisterProduct(pn, pb, pc, purl);
                            finish();
                        }


                    }
                }
        );


    }

    /**
     * Adds data filled in in form to database
     * @param productName name of the product to be added to database
     * etc...
     * @param productUrl deprecated database field that still needs to be added to database
     */
    public void RegisterProduct(String productName, String productBrand, String productContent, String productUrl)
    {
        Product.createProduct(new Product(Product.createProductUID(barcode),productName,productBrand, barcode, productUrl, productContent)); //product gaat aangemaakt worden
        final String stockId = LoginActivity.getCurrentStock();
        if (!stockId.equals("")) {
            Stock.addStockEntryToInStock(stockId, new StockEntry(Product.createProductUID(barcode), exdate));
            Stock.getRef(stockId).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Stock stock = dataSnapshot.getValue(Stock.class);
                    if (stock.getOut_stock().containsKey(Product.createProductUID(barcode))){
                        for (String key: stock.getOut_stock().keySet()){
                            Stock.removeFromOutStock(stockId,Product.createProductUID(barcode),key);
                            break;
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.product_succes, productName), Toast.LENGTH_SHORT).show();
        }else {
            //// TODO: 30-5-2017 user heeft geen current stock
        }
        Log.v("datalog", "barcode:"+barcode+", pn:"+productName+", pb:"+productBrand+", pc:"+productContent+", purl:"+productUrl);
        Log.v("datalog", "exdate: "+exdate);

    }

}