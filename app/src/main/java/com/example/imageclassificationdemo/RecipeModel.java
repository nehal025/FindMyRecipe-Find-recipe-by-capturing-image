package com.example.imageclassificationdemo;

import android.os.Parcel;

import java.io.Serializable;

public class RecipeModel  implements Serializable {



    private String Name;
    private String Recipe;
    private String BuyLink;
    private String ImageLink;


    public RecipeModel(String name, String recipe, String buyLink, String imageLink) {
        Name = name;
        Recipe = recipe;
        BuyLink = buyLink;
        ImageLink = imageLink;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getRecipe() {
        return Recipe;
    }

    public void setRecipe(String recipe) {
        Recipe = recipe;
    }

    public String getBuyLink() {
        return BuyLink;
    }

    public void setBuyLink(String buyLink) {
        BuyLink = buyLink;
    }

    public String getImageLink() {
        return ImageLink;
    }

    public void setImageLink(String imageLink) {
        ImageLink = imageLink;
    }




}
