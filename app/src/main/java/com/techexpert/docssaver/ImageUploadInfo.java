package com.techexpert.docssaver;

class ImageUploadInfo
{
    private String ImageUrl;
    private String type;

    public ImageUploadInfo(String imageUrl, String type) {
        ImageUrl = imageUrl;
        this.type = type;
    }

    public String getImageUrl() {
        return ImageUrl;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setImageUrl(String imageUrl) {
        ImageUrl = imageUrl;
    }
}
