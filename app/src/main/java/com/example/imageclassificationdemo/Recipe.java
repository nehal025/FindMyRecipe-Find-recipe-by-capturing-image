package com.example.imageclassificationdemo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.CollapsingToolbarLayout;

import java.util.ArrayList;

public class Recipe extends AppCompatActivity {

    ArrayList<RecipeModel> recipeModel= new ArrayList<>();
    ImageView imageView;
    TextView textView;
    TextView textView2;
    CollapsingToolbarLayout collapsingToolbarLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe);

        imageView=findViewById(R.id.img);
        textView=findViewById(R.id.name);
        textView2=findViewById(R.id.recipe);
        collapsingToolbarLayout=findViewById(R.id.collapsing_toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        recipeModel = (ArrayList<RecipeModel>) getIntent().getSerializableExtra("key");
        collapsingToolbarLayout.setTitle(recipeModel.get(0).getName());
        textView.setText(recipeModel.get(0).getName()+" recipe");
        textView2.setText(recipeModel.get(0).getRecipe());
        Glide.with(this).load(recipeModel.get(0).getImageLink()).into(imageView);

    }


}