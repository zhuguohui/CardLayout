# 效果
支持左右上 三个方向删除内容，支持下拉显示上一个。支持adapter 支持复用。

![在这里插入图片描述](https://img-blog.csdnimg.cn/ca082817b6b04e178f4c6af78b44051d.gif#pic_center)

## 支持通过API移除卡片
![在这里插入图片描述](https://img-blog.csdnimg.cn/2a227d06bb2c4fb2991f520da4a25f8d.gif#pic_center)


# 性能
性能优异，来来回回查看10个卡片。一共新建了6次。3个是页面中显示，2个是缓存。其他时候都是复用。
![在这里插入图片描述](https://img-blog.csdnimg.cn/0f21377cfb944cebb5ff442b4810b3ee.png)
后期都是绑定数据，复用View
![在这里插入图片描述](https://img-blog.csdnimg.cn/bffb86c1e6804c15a042491dede21750.png)

# 使用

```java
    myLayout.setAdapter(new StackAdapter() {
            final int[] bgColors=new int[]{Color.RED,Color.GREEN,Color.BLUE};
            @Override
            public View getView(int position, LayoutInflater inflater, ViewGroup viewGroup) {
                Log.i("zzz", "getView: position="+position);
                return inflater.inflate(R.layout.demo_item,viewGroup,false);
            }

            @Override
            public int getVisibleCount() {
                return 3;
            }

            @Override
            public int getCount() {
                return 10;
            }

            @Override
            public void bindData(View view, int position) {
                Log.i("zzz", "bindData: position="+position);
                TextView tv= (TextView) view;
                tv.setText("数据:"+position);
                tv.setBackgroundColor(bgColors[position%bgColors.length]);
            }
        });
```
# 代码
[CardLayout](https://github.com/zhuguohui/CardLayout/tree/master)
