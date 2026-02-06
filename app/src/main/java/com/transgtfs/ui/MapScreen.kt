package com.transgtfs.ui

import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import org.maplibre.gl.maps.MapLibreMap
import org.maplibre.gl.maps.MapView
import org.maplibre.gl.maps.Style
import org.maplibre.gl.style.layers.LineLayer
import org.maplibre.gl.style.layers.PropertyFactory.lineColor
import org.maplibre.gl.style.layers.PropertyFactory.lineWidth
import org.maplibre.gl.style.sources.GeoJsonSource

private const val ROUTE_SOURCE_ID = "route-source"
private const val ROUTE_LAYER_ID = "route-layer"

@Composable
fun MapScreen(
    styleUri: String,
    routePoints: List<Point> = emptyList(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context) }
    val mapLibreMap = remember { mutableStateOf<MapLibreMap?>(null) }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            mapView.apply {
                getMapAsync { map ->
                    mapLibreMap.value = map
                    map.setStyle(Style.Builder().fromUri(styleUri)) { style ->
                        ensureRouteLayer(style)
                    }
                }
            }
        }
    )

    LaunchedEffect(routePoints, mapLibreMap.value) {
        mapLibreMap.value?.getStyle { style ->
            ensureRouteLayer(style)
            val source = style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE_ID)
            val lineString = if (routePoints.isNotEmpty()) {
                LineString.fromLngLats(routePoints)
            } else {
                LineString.fromLngLats(emptyList())
            }
            source?.setGeoJson(lineString)
        }
    }
}

private fun ensureRouteLayer(style: Style) {
    if (style.getSource(ROUTE_SOURCE_ID) == null) {
        style.addSource(GeoJsonSource(ROUTE_SOURCE_ID, LineString.fromLngLats(emptyList())))
    }
    if (style.getLayer(ROUTE_LAYER_ID) == null) {
        val layer = LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
            lineColor(Color.parseColor("#007AFF")),
            lineWidth(4f)
        )
        style.addLayer(layer)
    }
}
