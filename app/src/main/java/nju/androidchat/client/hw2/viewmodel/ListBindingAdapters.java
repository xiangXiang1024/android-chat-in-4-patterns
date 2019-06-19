/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nju.androidchat.client.hw2.viewmodel;

import android.content.Context;
import android.graphics.Rect;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.databinding.BindingAdapter;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ObservableList;
import androidx.databinding.ViewDataBinding;
import androidx.databinding.adapters.ListenerUtil;

import java.util.ArrayList;
import java.util.List;

import nju.androidchat.client.BR;
import nju.androidchat.client.R;
import nju.androidchat.client.hw2.model.ClientMessageObservable;

/**
 * Contains a BindingAdapter for assigning a list of items to a ViewGroup.
 */
@lombok.extern.java.Log
public class ListBindingAdapters {
    private static final String TAG = "ListBindingAdapters";
    private static int scrollNum = 0;

    private static void resetScrollNum() {
        scrollNum = 0;
    }

    /**
     * Prevent instantiation
     */
    private ListBindingAdapters() {
    }

    /**
     * Assign a list of items to a ViewGroup. This is used with the {@code entries} and
     * {@code layout} attributes in the application namespace. Example Usage:
     * <pre><code>&lt;LinearLayout
     *     android:layout_width="match_parent"
     *     android:layout_height="wrap_content"
     *     android:orientation="vertical"
     *     app:entries="@{items}"
     *     app:layout="@{@layout/item}"/&gt;
     * </code></pre>
     * <p>
     * In the above, {@code items} is a List or ObservableList. {@code layout} does not
     * need to be hard-coded, but most commonly will be. This BindingAdapter will work
     * with any ViewGroup that only needs addView() and removeView() to manage its Views.
     * <p>
     * The layout, &commat;layout/item for example, must have a single variable named
     * {@code data}.
     */
    @BindingAdapter({"entries", "layout", "counter"})
    public static <T> void setEntries(ViewGroup viewGroup,
                                      List<T> oldEntries, int oldLayoutId, Counter oldCounter,
                                      List<T> newEntries, int newLayoutId, Counter newCounter) {
        if (oldEntries == newEntries && oldLayoutId == newLayoutId) {
            return; // nothing has changed
        }

        EntryChangeListener listener = ListenerUtil.getListener(viewGroup, R.id.entryListener);
        if (oldEntries != newEntries && listener != null && oldEntries instanceof ObservableList) {
            ((ObservableList<T>) oldEntries).removeOnListChangedCallback(listener);
        }

        if (newEntries == null) {
            viewGroup.removeAllViews();
        } else {
            if (newEntries instanceof ObservableList) {
                if (listener == null) {
                    listener =
                            new EntryChangeListener(viewGroup, newLayoutId, newCounter);
                    ListenerUtil.trackListener(viewGroup, listener,
                            R.id.entryListener);
                } else {
                    listener.setLayoutId(newLayoutId);
                }
                if (newEntries != oldEntries) {
                    ((ObservableList<T>) newEntries).addOnListChangedCallback(listener);
                }
            }
            resetViews(viewGroup, newLayoutId, newEntries, newCounter);
        }
    }

    /**
     * Inflates and binds a layout to an entry to the {@code data} variable
     * of the bound layout.
     *
     * @param inflater The LayoutInflater
     * @param parent   The ViewGroup containing the list of Views
     * @param layoutId The layout ID to use for the list item
     * @param entry    The data to bind to the inflated TalkView
     * @return A ViewDataBinding, bound to a newly-inflated TalkView with {@code entry}
     * set as the {@code data} variable.
     */
    private static ViewDataBinding bindLayout(LayoutInflater inflater,
                                              ViewGroup parent, int layoutId, Object entry) {
        ViewDataBinding binding = DataBindingUtil.inflate(inflater,
                layoutId, parent, false);
//        target.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
//            log.info("scroll");
//            if (!uiOperator.isInScreen(target) && !clientMessageObservable.isRead()) {
//                log.info("scroll and in screen");
//                clientMessageObservable.setRead(true);
//                counter.setMessageToReadNum(counter.getMessageToReadNum() - 1);
//            }
//        });
        if (!binding.setVariable(BR.messageBean, entry)) {
            String layoutName = parent.getResources().getResourceEntryName(layoutId);
            Log.w(TAG, "There is no variable 'data' in layout " + layoutName);
        }
        return binding;
    }

    /**
     * Clears all Views in {@code parent} and fills it with a TalkView for
     * each item in {@code entries}, bound to the item. If layoutId
     * is 0, no Views will be added.
     *
     * @param parent   The ViewGroup to contain the list of items.
     * @param layoutId The layout ID to inflate for the child Views.
     * @param entries  The list of items to bind to the inflated Views. Each
     *                 item will be bound to a different child TalkView.
     */


    private static void resetViews(ViewGroup parent, int layoutId,
                                   List entries, Counter counter) {
        parent.removeAllViews();
        if (layoutId == 0) {
            return;
        }
        LayoutInflater inflater = (LayoutInflater) parent.getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        for (int i = 0; i < entries.size(); i++) {
            Object entry = entries.get(i);
            ClientMessageObservable clientMessageObservable = (ClientMessageObservable) entry;
            ViewDataBinding binding = bindLayout(inflater, parent,
                    layoutId, entry);
            View target = binding.getRoot();
            parent.addView(target);
            target.post(() -> {
                if (isTotallyVisible(target)) {
//                    log.info("in func resetViews: in screen");
                    clientMessageObservable.setRead(true);
                } else {
//                    log.info("in func resetViews: not in screen");
                    clientMessageObservable.setRead(false);
                    counter.setMessageToReadNum(counter.getMessageToReadNum() + 1);
                }
            });

        }
        View scroller = (View) parent.getParent();
//        resetScrollNum();
        /*scroller.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            log.info("scroll");
            log.info("entries.size(): " + entries.size());
//            scrollNum++;
            counter.setMessageToReadNum(0);
            for (int i = 0; i < entries.size(); i++) {
                View target = parent.getChildAt(i);
                ClientMessageObservable clientMessageObservable = (ClientMessageObservable) entries.get(i);
                target.post(() -> {
                    if (!isTotallyVisible(target)) {
                        log.info("not in screen");
                        counter.setMessageToReadNum(counter.getMessageToReadNum() + 1);
                    }else {
                        log.info("in screen");
                    }
                    *//*if (isTotallyVisible(target) && !clientMessageObservable.isRead()) {
                        log.info("scroll and in screen");
                        clientMessageObservable.setRead(true);
                        counter.setMessageToReadNum(counter.getMessageToReadNum() - 1);
                    }*//*
                });
            }
            counter.setMessageToReadNum(counter.getMessageToReadNum() / 4);
        });*/
        scroller.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            //            private ThreadLocal<Integer> countNum = new ThreadLocal<>();
            private ThreadLocal<List<Integer>> indexList = new ThreadLocal<>();
            @Override
            public void onScrollChange(View view, int i, int i1, int i2, int i3) {
//                log.info("in the func onScrollChange");
//                countNum.set(0);
//                int countNum = 0;
                counter.setMessageToReadNum(0);
                indexList.set(new ArrayList<>());
//                log.info("entries.size(): " + entries.size());
                for (int index = 0; i < entries.size(); index++) {
//                    log.info("index: " + index);
                    if(index >= entries.size()) {
                        break;
                    }
                    final Integer index_i = (Integer) index;
                    View target = parent.getChildAt(index);
                    target.post(new Runnable() {
                        @Override
                        public void run() {
                            if (!isTotallyVisible(target)) {
//                                countNum.set(countNum.get()+1);
                                if(!indexList.get().contains(index_i)) {
                                    indexList.get().add(index_i);
                                }
                                log.info("not in screen");
                                if(counter.getMessageToReadNum() < indexList.get().size()) {
                                    counter.setMessageToReadNum(indexList.get().size());
                                }
//                                counter.setMessageToReadNum(counter.getMessageToReadNum() + 1);
                            }
                        }
                    });
                }
//                log.info("indexList.get().size(): " + indexList.get().size());
//                counter.setMessageToReadNum(indexList.get().size());
//                log.info("countNum: " + countNum);
            }
        });

    }

    /**
     * A listener to watch for changes in an Observable list and
     * animate the change.
     */
    private static class EntryChangeListener extends ObservableList.OnListChangedCallback {
        private final ViewGroup mTarget;
        private int mLayoutId;
        LayoutInflater inflater;
        Counter counter;

        EntryChangeListener(ViewGroup mTarget, int mLayoutId, Counter counter) {
            this.mTarget = mTarget;
            this.mLayoutId = mLayoutId;
            this.inflater = (LayoutInflater) mTarget.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.counter = counter;
        }

        void setLayoutId(int layoutId) {
            mLayoutId = layoutId;
        }

        @Override
        public void onChanged(ObservableList observableList) {
            resetViews(mTarget, mLayoutId, observableList, counter);
        }

        @Override
        public void onItemRangeChanged(ObservableList observableList,
                                       int start, int count) {
            if (mLayoutId == 0) {
                return;
            }

            final int end = start + count;
            for (int i = start; i < end; i++) {
                Object data = observableList.get(i);
                ClientMessageObservable clientMessageObservable = (ClientMessageObservable) data;
                ViewDataBinding binding = bindLayout(inflater,
                        mTarget, mLayoutId, data);
                binding.setVariable(BR.messageBean, observableList.get(i));
                mTarget.removeViewAt(i);
                View target = binding.getRoot();
                mTarget.addView(target, i);
                target.post(() -> {
                    if (isTotallyVisible(target)) {
//                        log.info("in func onItemRangeChanged: in screen");
                        clientMessageObservable.setRead(true);
                    } else {
//                        log.info("in func onItemRangeChanged: not in screen");
                        clientMessageObservable.setRead(false);
                        counter.setMessageToReadNum(counter.getMessageToReadNum() + 1);
                    }
                });

            }
        }

        @Override
        public void onItemRangeInserted(ObservableList observableList,
                                        int start, int count) {
            if (mLayoutId == 0) {
                return;
            }
            final int end = start + count;
//            counter.setMessageToReadNum(0);
            log.info("start: " + start + "  end: " + end + "  count: " + count);
            for (int i = end - 1; i >= start; i--) {
                Object entry = observableList.get(i);
                ClientMessageObservable clientMessageObservable = (ClientMessageObservable) entry;
                ViewDataBinding binding = bindLayout(inflater, mTarget, mLayoutId, entry);
                View target = binding.getRoot();
                mTarget.addView(target, start);
                target.post(() -> {
                    // TODO
                    /*log.info("in func onItemRangeInserted: ");
                    if(!isTotallyVisible(target)) {
                        log.info("target is invisible");
                        counter.setMessageToReadNum(counter.getMessageToReadNum() + 1);
                    }*/
                    if (isTotallyVisible(target)) {
//                        log.info("in func onItemRangeInserted: in screen");
                        clientMessageObservable.setRead(true);
                    } else {
//                        log.info("in func onItemRangeInserted: not in screen");
                        clientMessageObservable.setRead(false);
                        counter.setMessageToReadNum(counter.getMessageToReadNum() + 1);
                    }
                });

            }
        }

        @Override
        public void onItemRangeMoved(ObservableList observableList,
                                     int from, int to, int count) {
            if (mLayoutId == 0) {
                return;
            }
            for (int i = 0; i < count; i++) {
                View view = mTarget.getChildAt(from);
                mTarget.removeViewAt(from);
                int destination = (from > to) ? to + i : to;
                mTarget.addView(view, destination);
            }
        }

        @Override
        public void onItemRangeRemoved(ObservableList observableList,
                                       int start, int count) {
            if (mLayoutId == 0) {
                return;
            }
            for (int i = 0; i < count; i++) {
                mTarget.removeViewAt(start);
            }
        }
    }

    private static boolean isTotallyVisible(View view) {
        Rect rect = new Rect();
        view.getLocalVisibleRect(rect);
        return rect.top==0 && rect.bottom == view.getHeight();
    }
}