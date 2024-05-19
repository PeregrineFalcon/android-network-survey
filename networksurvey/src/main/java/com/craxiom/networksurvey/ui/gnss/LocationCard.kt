/*
 * Copyright (C) 2021 Sean J. Barbeau
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
package com.craxiom.networksurvey.ui.gnss

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Build
import android.text.TextUtils
import android.text.format.DateFormat
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.expandVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.craxiom.networksurvey.Application
import com.craxiom.networksurvey.R
import com.craxiom.networksurvey.model.DilutionOfPrecision
import com.craxiom.networksurvey.ui.gnss.data.FixState
import com.craxiom.networksurvey.ui.gnss.model.SatelliteMetadata
import com.craxiom.networksurvey.util.DateTimeUtils
import com.craxiom.networksurvey.util.FormatUtils.formatAccuracy
import com.craxiom.networksurvey.util.FormatUtils.formatAltitude
import com.craxiom.networksurvey.util.FormatUtils.formatAltitudeMsl
import com.craxiom.networksurvey.util.FormatUtils.formatBearing
import com.craxiom.networksurvey.util.FormatUtils.formatBearingAccuracy
import com.craxiom.networksurvey.util.FormatUtils.formatDoP
import com.craxiom.networksurvey.util.FormatUtils.formatHvDOP
import com.craxiom.networksurvey.util.FormatUtils.formatLatOrLon
import com.craxiom.networksurvey.util.FormatUtils.formatNumSats
import com.craxiom.networksurvey.util.FormatUtils.formatSpeed
import com.craxiom.networksurvey.util.FormatUtils.formatSpeedAccuracy
import com.craxiom.networksurvey.util.IOUtils
import com.craxiom.networksurvey.util.PreferenceUtils.gnssFilter
import com.craxiom.networksurvey.util.SatelliteUtil.isVerticalAccuracySupported
import com.craxiom.networksurvey.util.UIUtils
import java.text.SimpleDateFormat

@Preview
@Composable
fun LocationCardPreview(
    @PreviewParameter(LocationPreviewParameterProvider::class) location: Location
) {
    LocationCard(
        location,
        "5 sec",
        1.4,
        DilutionOfPrecision(1.0, 2.0, 3.0),
        SatelliteMetadata(),
        FixState.Acquired
    )
}

class LocationPreviewParameterProvider : PreviewParameterProvider<Location> {
    override val values = sequenceOf(previewLocation())
}

fun previewLocation(): Location {
    val l = Location("preview")
    l.apply {
        latitude = 28.38473847
        longitude = -87.32837456
        time = 1633375741711
        altitude = 13.5
        speed = 21.5f
        bearing = 240f
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            bearingAccuracyDegrees = 5.6f
            speedAccuracyMetersPerSecond = 6.1f
            verticalAccuracyMeters = 92.5f
        }
    }
    return l
}

@Composable
fun LocationCard(
    location: Location,
    ttff: String,
    altitudeMsl: Double,
    dop: DilutionOfPrecision,
    satelliteMetadata: SatelliteMetadata,
    fixState: FixState,
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp)
            .clickable {
                copyToClipboard(context, location)
            },
        elevation = CardDefaults.cardElevation(),
        colors = CardDefaults.elevatedCardColors()
    ) {
        Box {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
            ) {
                LabelColumn1()
                ValueColumn1(context, location, altitudeMsl, dop)
                LabelColumn2(location)
                ValueColumn2(location, ttff, dop, satelliteMetadata)
            }
            LockIcon(fixState)
        }
    }
}

@Composable
fun ValueColumn1(
    context: Context,
    location: Location,
    altitudeMsl: Double,
    dop: DilutionOfPrecision,
) {
    Column(
        modifier = Modifier
            .wrapContentHeight()
            .wrapContentWidth()
            .padding(top = 5.dp, bottom = 5.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Latitude(location)
        Longitude(location)
        Altitude(location)
        AltitudeMsl(altitudeMsl)
        Speed(location)
        SpeedAccuracy(location)
        Pdop(dop)
    }
}

@Composable
fun Latitude(location: Location) {
    LocationValue(formatLatOrLon(Application.get(), location.latitude))
}

@Composable
fun Longitude(location: Location) {
    LocationValue(formatLatOrLon(Application.get(), location.longitude))
}

@Composable
fun Altitude(location: Location) {
    LocationValue(formatAltitude(Application.get(), location))
}

@Composable
fun AltitudeMsl(altitudeMsl: Double) {
    LocationValue(formatAltitudeMsl(Application.get(), altitudeMsl))
}

@Composable
fun Speed(location: Location) {
    LocationValue(formatSpeed(Application.get(), location))
}

@Composable
fun SpeedAccuracy(location: Location) {
    LocationValue(formatSpeedAccuracy(Application.get(), location))
}

@Composable
fun Pdop(dop: DilutionOfPrecision) {
    LocationValue(formatDoP(Application.get(), dop = dop))
}

@Composable
fun ValueColumn2(
    location: Location,
    ttff: String,
    dop: DilutionOfPrecision,
    satelliteMetadata: SatelliteMetadata,
) {
    Column(
        modifier = Modifier
            .wrapContentHeight()
            .wrapContentWidth()
            .padding(top = 5.dp, bottom = 5.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Time(location)
        TTFF(ttff)
        Accuracy(location)
        NumSats(satelliteMetadata)
        Bearing(location)
        BearingAccuracy(location)
        HVDOP(dop)
    }
}

@Composable
fun Time(location: Location) {
    if (location.time == 0L) {
        LocationValue("")
    } else {
        formatTime(location.time)
    }
}

@Composable
private fun formatTime(time: Long) {
    // SimpleDateFormat can only do 3 digits of fractional seconds (.SSS)
    val SDF_TIME_24_HOUR = "HH:mm:ss"
    val SDF_TIME_12_HOUR = "hh:mm:ss a"
    val SDF_DATE_24_HOUR = "HH:mm:ss MMM d, yyyy z"
    val SDF_DATE_12_HOUR = "hh:mm:ss a MMM d, yyyy z"

    // See #117
    @SuppressLint("SimpleDateFormat")
    val timeFormat = remember {
        SimpleDateFormat(
            if (DateFormat.is24HourFormat(Application.get().applicationContext)) SDF_TIME_24_HOUR else SDF_TIME_12_HOUR
        )
    }

    @SuppressLint("SimpleDateFormat")
    val timeAndDateFormat = remember {
        SimpleDateFormat(
            if (DateFormat.is24HourFormat(Application.get().applicationContext)) SDF_DATE_24_HOUR else SDF_DATE_12_HOUR
        )
    }

    if (LocalConfiguration.current.screenWidthDp > 450 || !DateTimeUtils.isTimeValid(time)) { // 450dp is a little larger than the width of a Samsung Galaxy S8+
        val dateAndTime = timeAndDateFormat.format(time).trimZeros()
        // Time and date
        if (DateTimeUtils.isTimeValid(time)) {
            LocationValue(dateAndTime)
        } else {
            ErrorTime(dateAndTime, time)
        }
    } else {
        // Time
        LocationValue(timeFormat.format(time).trimZeros())
    }
}

private fun String.trimZeros(): String {
    return this.replace(".000", "")
        .replace(",000", "")
}

@Composable
fun TTFF(ttff: String) {
    LocationValue(ttff)
}

/**
 * Horizontal and vertical location accuracies based on the provided location
 * @param location
 */
@Composable
fun Accuracy(location: Location) {
    LocationValue(formatAccuracy(Application.get(), location))
}

@Composable
fun NumSats(satelliteMetadata: SatelliteMetadata) {
    val fontStyle = if (gnssFilter(Application.get(), Application.getPrefs()).isNotEmpty()) {
        // Make text italic so it matches filter text
        FontStyle.Italic
    } else {
        FontStyle.Normal
    }
    LocationValue(
        formatNumSats(Application.get(), satelliteMetadata),
        fontStyle
    )
}

@Composable
fun Bearing(location: Location) {
    LocationValue(formatBearing(Application.get(), location))
}

@Composable
fun BearingAccuracy(location: Location) {
    LocationValue(formatBearingAccuracy(Application.get(), location))
}

@Composable
fun HVDOP(dop: DilutionOfPrecision) {
    LocationValue(formatHvDOP(Application.get(), dop))
}

@Composable
fun LabelColumn1() {
    Column(
        modifier = Modifier
            .wrapContentHeight()
            .wrapContentWidth()
            .padding(top = 5.dp, bottom = 5.dp, start = 5.dp, end = 5.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.End
    ) {
        LocationLabel(R.string.gps_latitude_label)
        LocationLabel(R.string.gps_longitude_label)
        LocationLabel(R.string.gps_altitude_label)
        LocationLabel(R.string.gps_altitude_msl_label)
        LocationLabel(R.string.gps_speed_label)
        LocationLabel(R.string.gps_speed_acc_label)
        LocationLabel(R.string.pdop_label)
    }
}

@Composable
fun LabelColumn2(location: Location) {
    Column(
        modifier = Modifier
            .wrapContentHeight()
            .wrapContentWidth()
            .padding(top = 5.dp, bottom = 5.dp, end = 5.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.End
    ) {
        LocationLabel(R.string.gps_fix_time_label)
        LocationLabel(R.string.gps_ttff_label)
        LocationLabel(if (location.isVerticalAccuracySupported()) R.string.gps_hor_and_vert_accuracy_label else R.string.gps_accuracy_label)
        LocationLabel(R.string.gps_num_sats_label)
        LocationLabel(R.string.gps_bearing_label)
        LocationLabel(R.string.gps_bearing_acc_label)
        LocationLabel(R.string.hvdop_label)
    }
}

@Composable
fun LocationLabel(@StringRes id: Int) {
    if (reduceSpacing()) {
        Text(
            text = stringResource(id),
            modifier = Modifier.padding(start = 4.dp, end = 4.dp),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            letterSpacing = letterSpacing(),
        )
    } else {
        Text(
            text = stringResource(id),
            modifier = Modifier.padding(start = 4.dp, end = 4.dp),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
        )
    }
}

@Composable
fun LocationValue(text: String, fontStyle: FontStyle = FontStyle.Normal) {
    if (reduceSpacing()) {
        Text(
            text = text,
            modifier = Modifier.padding(end = 2.dp),
            fontSize = 13.sp,
            letterSpacing = letterSpacing(),
            fontStyle = fontStyle
        )
    } else {
        Text(
            text = text,
            modifier = Modifier.padding(end = 4.dp),
            fontSize = 13.sp,
            fontStyle = fontStyle
        )
    }
}

@Composable
fun ErrorTime(timeText: String, timeMs: Long) {
    var openDialog = remember { mutableStateOf(false) }
    // Red time box
    Box(
        Modifier
            .wrapContentHeight()
            .wrapContentWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.error)
            .clickable {
                openDialog.value = true
            }
    ) {
        Text(
            text = timeText,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onError
        )
    }

    // Alert Dialog
    val format = remember {
        SimpleDateFormat.getDateTimeInstance(
            java.text.DateFormat.LONG,
            java.text.DateFormat.LONG
        )
    }

    if (openDialog.value) {
        AlertDialog(
            onDismissRequest = {
                openDialog.value = false
            },
            title = {
                Text(stringResource(R.string.gps_error_time_title))
            },
            text = {
                Column {
                    LinkifyText(
                        text = Application.get().getString(
                            R.string.gps_error_time_message, format.format(timeMs),
                            DateTimeUtils.NUM_DAYS_TIME_VALID
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        openDialog.value = false
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
}

@Composable
private fun reduceSpacing(): Boolean {
    return LocalConfiguration.current.screenWidthDp < 365 // Galaxy S21+ is 384dp wide, Galaxy S8 is 326dp
}

private fun letterSpacing(): TextUnit {
    // Reduce text spacing on narrow displays to make both columns fit
    return (-0.01).em
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LockIcon(fixState: FixState) {
    var visible by remember { mutableStateOf(false) }
    visible = fixState == FixState.Acquired
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn() + expandVertically(expandFrom = Alignment.CenterVertically),
        exit = scaleOut() + shrinkVertically(shrinkTowards = Alignment.CenterVertically)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_baseline_lock_24),
            contentDescription = stringResource(id = R.string.lock),
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(6.dp)
        )
    }
}

private fun copyToClipboard(context: Context, location: Location) {
    if (location.latitude == 0.0 && location.longitude == 0.0) return
    val formattedLocation = UIUtils.formatLocationForDisplay(
        location, null, true,
        null, null, null, context.getString(R.string.preferences_coordinate_format_dd_key)
    )
    if (!TextUtils.isEmpty(formattedLocation)) {
        IOUtils.copyToClipboard(formattedLocation)
        // Android 12 and higher generates a Toast automatically
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_LONG)
                .show()
        }
    }
}