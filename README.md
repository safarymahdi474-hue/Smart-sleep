# خواب هوشمند (Smart Sleep)

اپلیکیشن native Android (Kotlin + Jetpack Compose, معماری MVVM + Repository).

## اجرای پروژه

1. Android Studio (Koala یا جدیدتر) را باز کنید → Open → پوشه `SmartSleepApp` را انتخاب کنید.
2. Studio به‌صورت خودکار Gradle Wrapper (`gradle-wrapper.jar`) را در صورت نبود دانلود می‌کند.
   (`gradlew` موجود در ریشه پروژه فقط یک اسکریپت راه‌انداز است؛ خود jar به دلیل باینری بودن در
   این خروجی گنجانده نشده — Studio یا دستور `gradle wrapper` آن را می‌سازد.)
3. Run را بزنید یا از ترمینال: `./gradlew assembleDebug`

خروجی APK در: `app/build/outputs/apk/debug/app-debug.apk`

## نکات فنی کلیدی

- **الگوریتم چرخه خواب:** `domain/SleepCycleCalculator.kt` — خالص، بدون وابستگی به Android،
  دقیقاً طبق ۶ مرحله‌ی درخواستی (ثبت شروع خواب → تاخیر به خواب رفتن → چرخه‌های ۹۰ دقیقه‌ای
  قابل تنظیم → فیلتر بازه بیداری → انتخاب بهترین گزینه).
- **یادگیری:** `domain/AdaptiveLearningEngine.kt` — بعد از هر امتیازدهی، دو مقدار را کمی تنظیم
  می‌کند: تاخیر تخمینی خواب رفتن، و ترجیح موقعیت داخل بازه (زودتر/دیرتر). هیچ ادعای پزشکی ندارد.
- **آلارم:** `alarm/AlarmScheduler.kt` از `AlarmManager.setAlarmClock()` استفاده می‌کند — API
  رسمی اندروید برای اپ‌های آلارم که هم از Doze/App Standby معاف است و هم نیاز به مجوز ویژه
  Exact Alarm در اندروید ۱۲+ ندارد.
- **پایداری آلارم:** `alarm/BootReceiver.kt` بعد از ریست شدن گوشی، تغییر ساعت سیستم، تغییر
  منطقه زمانی یا آپدیت اپ، آلارم فعال را دوباره زمان‌بندی می‌کند.
- **Smart Wake:** `alarm/SmartWakeMonitorService.kt` — تشخیص بهترین‌تلاش بر پایه شتاب‌سنج؛
  در صورت نبود سنسور مناسب، بدون ادعای دقت پزشکی به الگوریتم چرخه خواب برمی‌گردد.
- **ذخیره‌سازی محلی:** Room برای تاریخچه خواب (`data/local/`)، DataStore برای تنظیمات —
  بدون نیاز به اینترنت.
- **RTL و دو زبانه:** `values/strings.xml` (انگلیسی) و `values-fa/strings.xml` (فارسی پیش‌فرض)؛
  `android:supportsRtl="true"` در Manifest.

## مجوزهای اندروید مدیریت‌شده

- `POST_NOTIFICATIONS` (اندروید ۱۳+): در `MainActivity` درخواست می‌شود.
- Battery optimization: در صفحه تنظیمات دکمه‌ای برای معافیت اپ وجود دارد.
- `USE_FULL_SCREEN_INTENT` و `WAKE_LOCK` برای نمایش صفحه آلارم روی صفحه قفل.

## محدودیت شناخته‌شده

`gradle-wrapper.jar` باینری در این خروجی متنی گنجانده نشده؛ اولین باز کردن پروژه در
Android Studio یا اجرای `gradle wrapper` آن را می‌سازد.
