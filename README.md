# arkImageLoader
Image downloading Library for Listview

Features:

> Parallel image downloading by dividing image in multiple chunks

> Image Cashing

> Optimised image loading for ListViews/GridViews



Usage:

1> In ListView Custom Adapter create an object of ImageManager

            ImageManager imageManager;
            
            imageManager = new ImageManager(Context : context, View : listView, int : cache_duration);
                                    

2> In getView() method make a call to displayImage with following parametern

            imageManager.displayImage(String : IMAGE_URL, ImageView : IMAGE_VIEW, int : PLACE HOLDER DRAWABLE);



