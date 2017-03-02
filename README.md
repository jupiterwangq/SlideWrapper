SlideWrapper
===============
# 说明
Android上仿QQ侧滑删除组件，任意布局用该组件包装后即可实现侧滑

# 效果
* 动态模式
   ![](https://github.com/jupiterwangq/SlideWrapper/blob/master/effect1.gif)
* 静态模式
   ![](https://github.com/jupiterwangq/SlideWrapper/blob/master/effect2.gif)
# 使用方法
在布局中像下面这样把自己的view用SlideWrapper包装起来即可：
```Java
<com.jupiter.SlideWrapper xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:sw="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    sw:mode="dynamic"
    sw:anim_duration="300">
    <!-- 将侧滑后展开的各个view也一并加进来，侧滑块加上android:tag="ctrl"作为标记-->
    <TextView
        android:id="@+id/top"
        android:layout_width="80dp"
        android:layout_height="wrap_content"
        android:text="置顶"
        android:gravity="center"
        android:paddingLeft="8dp"
        android:paddingRight="8dp"
        android:textColor="#ffffff"
        android:background="@android:color/darker_gray"
        android:tag="ctrl"/>
    <TextView
        android:id="@+id/delete"
        android:layout_width="90dp"
        android:layout_height="wrap_content"
        android:tag="ctrl"
        android:text="删除"
        android:gravity="center"
        android:paddingLeft="8dp"
        android:paddingRight="8dp"
        android:textColor="#ffffff"
        android:background="#ff0000"/>
    <!-- 最后放item -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="80dp"
        android:background="#00ff00"
        android:gravity="center_vertical">
        <TextView
            android:id="@+id/text"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content" />
    </LinearLayout>
</com.jupiter.SlideWrapper>
```

# 缺点
因为使用了包装布局的方式，会增加布局的嵌套层次
