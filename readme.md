# Agenda - Read me

## Config

| Key | Type | Description | Default value |
| --- | --- | --- | --- |
| `maps_key` | String | The Google Maps API key required for GeoLocation. | - |
| `weather_key` | String | The API key required to fetch weather information.<br><br>Weather information cannot be collected if an invalid `maps_key` is provided. | - |
| `ptv_dev_id` | Integer | The developer id which makes up half of the credentials required to access the PTV API. | - |
| `ptv_key` | String | The key which makes up the other half of the credentials required to access the PTV API. | - |
| `timezone_offtset` | Integer | The hour offset based on the users's location. If timezone is GMT+10:00 then set this to 10.<br><br>Must be a value between 0 and 23 inclusive. | 10 |
| `default_location` | String | The default location to collect weather information for if an event has no location provided. | Melbourne VIC, Australia |
| `default_primary_alias` | String | An alias to replace the user's email address when displaying events from their primary calendar. | Personal |
| `default_calendar_colour` | Integer | The Google colour id of the default calendar colour. Used to assign a colour to the users tasks lists as the API does not return list colour values.<br><br>Must be a value between 1 and 24 inclusive.| 15 |
| `modern_colours` | Boolean | Specifies what colour list to use. If set to `true`, event labels will use the modern colour list. If set to `false`, event labels will use the classic colour list. | true |
| `latest_departure_buffer` | Integer | Specifies the minimum amount of time between the start of an event and the latest possible departure for PTV timetabling data. | 60 |
| `rmit_location` | Boolean | If set to true, event locations matching the RMIT room id format will be parsed into readable text. | false |
