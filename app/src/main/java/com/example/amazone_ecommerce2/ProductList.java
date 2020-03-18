package com.example.amazone_ecommerce2;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.amazone_ecommerce2.Common.Common;
import com.example.amazone_ecommerce2.Interface.ItemClickListener;
import com.example.amazone_ecommerce2.Model.Category;
import com.example.amazone_ecommerce2.Model.Product;
import com.example.amazone_ecommerce2.ViewHolder.ProductViewHolder;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.squareup.picasso.Picasso;

import java.util.UUID;

import info.hoang8f.widget.FButton;

public class ProductList extends AppCompatActivity {

    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;

    FloatingActionButton fab;

    FirebaseDatabase db;
    DatabaseReference productList;
    FirebaseStorage storage;
    StorageReference storageReference;

    String categoryId="";
    FirebaseRecyclerAdapter<Product, ProductViewHolder> adapter;

    MaterialEditText edtName,edtDescription,edtPrice,edtDiscount;
    FButton btnSelect,btnUpload;

    Product newProduct;

    RelativeLayout rootLayout;

    Uri saveUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_list);

        db=FirebaseDatabase.getInstance();
        productList=db.getReference("Subcategory");
        storage=FirebaseStorage.getInstance();
        storageReference=storage.getReference();

        recyclerView=findViewById(R.id.recycler_product);
        recyclerView.setHasFixedSize(true);
        layoutManager=new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        rootLayout=findViewById(R.id.rootLayout);

        fab=findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddProductDialog();
            }
        });

        if (getIntent()!=null)
            categoryId=getIntent().getStringExtra("CategoryId");
        if (!categoryId.isEmpty())
            loadListProduct(categoryId);
    }

    private void showAddProductDialog() {
        AlertDialog.Builder alertDialog=new AlertDialog.Builder(ProductList.this);
        alertDialog.setTitle("Add new Product");
        alertDialog.setMessage("Please fill full information");

        LayoutInflater inflater=this.getLayoutInflater();
        View add_menu_layout=inflater.inflate(R.layout.add_new_product_layout,null);

        edtName=add_menu_layout.findViewById(R.id.edtName);
        edtDescription=add_menu_layout.findViewById(R.id.edtDescription);
        edtPrice=add_menu_layout.findViewById(R.id.edtPrice);
        edtDiscount=add_menu_layout.findViewById(R.id.edtDiscount);

        btnSelect=add_menu_layout.findViewById(R.id.btnSelect);
        btnUpload=add_menu_layout.findViewById(R.id.btnUpload);
        btnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseImage();
            }
        });

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadImage();
            }
        });

        alertDialog.setView(add_menu_layout);
        alertDialog.setIcon(R.drawable.ic_shopping_cart_black_24dp);

        alertDialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                //here just create new category
                if (newProduct!=null)
                {
                    productList.push().setValue(newProduct);
                    Snackbar.make(rootLayout,"New category "+newProduct.getName()+" was added",Snackbar.LENGTH_SHORT).show();
                }

            }
        });
        alertDialog.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alertDialog.show();
    }

    private void uploadImage() {
        if (saveUri !=null)
        {
            final ProgressDialog mDialog=new ProgressDialog(this);
            mDialog.setMessage("Uploading...");
            mDialog.show();

            String imageName= UUID.randomUUID().toString();
            final StorageReference imageFolder=storageReference.child("images/"+imageName);
            imageFolder.putFile(saveUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            mDialog.dismiss();
                            Toast.makeText(ProductList.this,"Uploaded !!!",Toast.LENGTH_SHORT).show();
                            imageFolder.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    newProduct=new Product();
                                    newProduct.setName(edtName.getText().toString());
                                    newProduct.setDescription(edtDescription.getText().toString());
                                    newProduct.setPrice(edtPrice.getText().toString());
                                    newProduct.setDiscount(edtDiscount.getText().toString());
                                    newProduct.setMenuId(categoryId);
                                    newProduct.setImage(uri.toString());
                                }
                            });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            mDialog.dismiss();
                            Toast.makeText(ProductList.this,""+e.getMessage(),Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            double progress=(100.0 * taskSnapshot.getBytesTransferred()/taskSnapshot.getTotalByteCount());
                            mDialog.setMessage("Uploaded "+progress+"%");
                        }
                    });
        }
    }

    private void chooseImage() {

        Intent intent=new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent,"Select Picture"), Common.PICK_IMAGE_REQUEST);
    }

    private void loadListProduct(String categoryId) {
        Query listProductByCategoryId=productList.orderByChild("menuId").equalTo(categoryId);
        FirebaseRecyclerOptions<Product> options=new FirebaseRecyclerOptions.Builder<Product>()
                .setQuery(listProductByCategoryId,Product.class)
                .build();

        adapter=new FirebaseRecyclerAdapter<Product, ProductViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull ProductViewHolder viewHolder, int position, @NonNull Product model) {
                viewHolder.product_name.setText(model.getName());
                Picasso.with(getBaseContext())
                        .load(model.getImage())
                        .into(viewHolder.product_image);

                viewHolder.setItemClickListener(new ItemClickListener() {
                    @Override
                    public void onClick(View view, int position, boolean isLongClick) {
                        //code late
                    }
                });
            }

            @NonNull
            @Override
            public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View itemView=LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.product_item,parent,false);
                return new ProductViewHolder(itemView);
            }
        };
        adapter.startListening();

        adapter.notifyDataSetChanged();
        recyclerView.setAdapter(adapter);

    }

    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==Common.PICK_IMAGE_REQUEST && resultCode==RESULT_OK
                && data!=null && data.getData()!=null)
        {
            saveUri=data.getData();
            btnSelect.setText("Image Selected !");
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getTitle().equals(Common.UPDATE))
        {
            showUpdateProductDialog(adapter.getRef(item.getOrder()).getKey(),adapter.getItem(item.getOrder()));
        }
        else if (item.getTitle().equals(Common.DELETE))
        {
            deleteProduct(adapter.getRef(item.getOrder()).getKey());
        }
        return super.onContextItemSelected(item);
    }

    private void deleteProduct(String key) {
        productList.child(key).removeValue();
    }

    private void showUpdateProductDialog(final String key, final Product item) {
        AlertDialog.Builder alertDialog=new AlertDialog.Builder(ProductList.this);
        alertDialog.setTitle("Edit Product");
        alertDialog.setMessage("Please fill full information");

        LayoutInflater inflater=this.getLayoutInflater();
        View add_menu_layout=inflater.inflate(R.layout.add_new_product_layout,null);

        edtName=add_menu_layout.findViewById(R.id.edtName);
        edtDescription=add_menu_layout.findViewById(R.id.edtDescription);
        edtPrice=add_menu_layout.findViewById(R.id.edtPrice);
        edtDiscount=add_menu_layout.findViewById(R.id.edtDiscount);

        //set default value for view
        edtName.setText(item.getName());
        edtDescription.setText(item.getDescription());
        edtPrice.setText(item.getPrice());
        edtDiscount.setText(item.getDiscount());

        btnSelect=add_menu_layout.findViewById(R.id.btnSelect);
        btnUpload=add_menu_layout.findViewById(R.id.btnUpload);
        btnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseImage();
            }
        });

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeImage(item);
            }
        });

        alertDialog.setView(add_menu_layout);
        alertDialog.setIcon(R.drawable.ic_shopping_cart_black_24dp);

        alertDialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                    item.setName(edtName.getText().toString());
                    item.setDescription(edtDescription.getText().toString());
                    item.setPrice(edtPrice.getText().toString());
                    item.setDiscount(edtDiscount.getText().toString());


                    productList.child(key).setValue(item);

                    Snackbar.make(rootLayout,"Product "+item.getName()+" was edited",Snackbar.LENGTH_SHORT).show();

            }
        });
        alertDialog.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alertDialog.show();
    }

    private void changeImage(final Product item) {
        if (saveUri !=null)
        {
            final ProgressDialog mDialog=new ProgressDialog(this);
            mDialog.setMessage("Uploading...");
            mDialog.show();

            String imageName= UUID.randomUUID().toString();
            final StorageReference imageFolder=storageReference.child("images/"+imageName);
            imageFolder.putFile(saveUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            mDialog.dismiss();
                            Toast.makeText(ProductList.this,"Uploaded !!!",Toast.LENGTH_SHORT).show();
                            imageFolder.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    item.setImage(uri.toString());
                                }
                            });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            mDialog.dismiss();
                            Toast.makeText(ProductList.this,""+e.getMessage(),Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            double progress=(100.0 * taskSnapshot.getBytesTransferred()/taskSnapshot.getTotalByteCount());
                            mDialog.setMessage("Uploaded "+progress+"%");
                        }
                    });
        }
    }
}