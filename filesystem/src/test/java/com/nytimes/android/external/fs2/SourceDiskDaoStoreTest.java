package com.nytimes.android.external.fs2;


import com.google.gson.Gson;
import com.nytimes.android.external.store2.base.Fetcher;
import com.nytimes.android.external.store2.base.impl.BarCode;
import com.nytimes.android.external.store2.base.impl.Store;
import com.nytimes.android.external.store2.base.impl.StoreBuilder;
import com.nytimes.android.external.store2.middleware.GsonSourceParser;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;

import io.reactivex.Observable;
import okio.BufferedSource;
import okio.Okio;

import static com.google.common.base.Charsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SourceDiskDaoStoreTest {
    public static final String KEY = "key";
    @Mock
    Fetcher<BufferedSource, BarCode> fetcher;
    @Mock
    SourcePersister diskDAO;
    private final BarCode barCode = new BarCode("value", KEY);


    private static BufferedSource source(String data) {
        return Okio.buffer(Okio.source(new ByteArrayInputStream(data.getBytes(UTF_8))));
    }

    @Test
    public void testSimple() {
        MockitoAnnotations.initMocks(this);
        GsonSourceParser<Foo> parser = new GsonSourceParser<>(new Gson(), Foo.class);
        Store<Foo, BarCode> store = StoreBuilder.<BarCode, BufferedSource, Foo>parsedWithKey()
                .persister(diskDAO)
                .fetcher(fetcher)
                .parser(parser)
                .open();

        Foo foo = new Foo();
        foo.bar = barCode.getKey();

        String sourceData = new Gson().toJson(foo);


        BufferedSource source = source(sourceData);
        Observable<BufferedSource> value = Observable.just(source);
        when(fetcher.fetch(barCode))
                .thenReturn(value);

        when(diskDAO.read(barCode))
                .thenReturn(Observable.<BufferedSource>empty())
                .thenReturn(value);

        when(diskDAO.write(barCode, source))
                .thenReturn(Observable.just(true));

        Foo result = store.get(barCode).blockingFirst();
        assertThat(result.bar).isEqualTo(KEY);
        result = store.get(barCode).blockingFirst();
        assertThat(result.bar).isEqualTo(KEY);
        verify(fetcher, times(1)).fetch(barCode);
    }

    private static class Foo {
        String bar;

        Foo() {
        }
    }

}
