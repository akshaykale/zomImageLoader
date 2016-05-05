# arkImageLoader
Initial Commit: Image Loading Library for Listview



Usage:

1> In ListView Custom Adapter create an object of ImageManager

            ImageManager imageManager;
            
            imageManager = new ImageManager(Context : context, View : listView, int : cache_duration);
                                    

2> In getView() method make a call to displayImage with following parametern

            imageManager.displayImage(String : IMAGE_URL, ImageView : IMAGE_VIEW, int : PLACE HOLDER DRAWABLE);



