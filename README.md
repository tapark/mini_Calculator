# 단순한 계산기
***
### TextView 에 String 추가/제거
~~~kotlin
TextView.append(string) // 뒤에 string 추가
TextView.text.dropLast(3) // 뒤에서 3개 del
// dropLast는 List에서도 유용하게 사용할듯
~~~

### Local Database Room
~~~kotlin
// in build.gradle
dependencies {
	implementation "androidx.room:room-runtime:2.2.6"
	kapt "androidx.room:room-compiler:2.2.6"
} // 추가

// create history.kt / data class 생성
@Entity (tableName = "history_table")
data class  History(
	@PrimaryKey(autoGenerate = true) val uid: Int?,
	@ColumnInfo(name = "expression") val expression: String?,
	@ColumnInfo(name = "result") val result: String?
)

// create historyDao.kt / @Entity에 쿼리로 접근할 수 있는 interface 생성
@Dao
interface HistoryDao {

	@Query("SELECT * FROM history")
	fun getAll(): List<History>

	@Insert
	fun insertHistory(history: History)

	@Query("DELETE FROM history")
	fun deleteAll()
}

// in Activity.kt / AppDatabase 생성, 빌드
var db: AppDatabase = Room.databaseBuilder(
	applicationContext,
	AppDatabase::class.java,
	"HistoryDB"
).build()
~~~

### DB 작업은 별도의 Thread에서 Runnable 실행
~~~kotlin
Thread (Runnable {
	db.historyDao().insertHistory(History(null, expressionText, resultText))
}).start() // DB 추가

Thread(Runnable {
	db.historyDao().deleteAll()
}).start() // all_DB 삭제

// DB, Network 등 무거운 작업은 MainThread(UI)에 영향을 미치기 때문에 별도의 Thread에서 수행
~~~

### 별도의 Thread에서 MainThread UI 에 접근하려면?
 - Thread 의 Runnable 에서 runOnUiThread { }
~~~kotlin
Thread(Runnable {
	db.historyDao().getAll().reversed().forEach {
		runOnUiThread {
			val historyView = LayoutInflater.from(this).inflate(R.layout.history_row, null, false)
			historyView.findViewById<TextView>(R.id.expressionTextView).text = it.expression
			historyView.findViewById<TextView>(R.id.resultTextView).text = "= ${it.result}"

					historyLinearLayout.addView(historyView)
				}
			}
		}).start()
~~~

### View와 LayoutInflater
 - 기본적으로 setContentView()를 통해 View 가 호출됨
 - setContentView() 이후에 View를 추가 해야 한다면?
~~~ kotlin
// LayoutInflater class를 호출
val historyView = LayoutInflater.from(this).inflate(R.layout.history_row, null, false)
// Text 입력
historyView.findViewById<TextView>(R.id.expressionTextView).text = it.expression
historyView.findViewById<TextView>(R.id.resultTextView).text = "= ${it.result}"
// 뷰 추가 (addView)
historyLinearLayout.addView(historyView)
~~~

### MainActivity.kt 에서 TextView 효과 일부를 변경
~~~kotlin
val ssb = SpannableStringBuilder(expressionTextView.text)
ssb.setSpan(
    ForegroundColorSpan(getColor(R.color.blue)),
    expressionTextView.text.length - 2,
    expressionTextView.text.length,
    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

    expressionTextView.text = ssb
// setSpan(효과, 시작인덱스, 끝인덱스, 적용범위)
// ForegroundColorSpan : 텍스트 색상
// BackgroundColorSpan : 배경 색상
// UnderlineSpan : 텍스트 밑줄
// AbsoluteSizeSpan : 텍스트 크기
// StyleSpan : NORMAL, ITALIC, BOLD, BOLD_ITALIC
// SPAN_EXCLUSIVE_EXCLUSIVE : 왼쪽 제거, 오른쪽 제거
// SPAN_EXCLUSIVE_INCLUSIVE : 왼쪽 제거, 오른쪽 포함
// SPAN_INCLUSIVE_EXCLUSIVE : 왼쪽 포함, 오른쪽 제거
// SPAN_INCLUSIVE_INCLUSIVE : 왼쪽 포함, 오른쪽 포함
~~~

### Button의 Animation 효과
~~~kotlin
// in activity_main.xml
android:stateListAnimator=""
~~~

### activity_main.xml 에서 바로 버튼 동작을 정의
~~~kotlin
// in activity_main.xml
android:onClick="buttonClicked"

// in MainActivity.kt 에서 함수 정의
// findViewById 와 setOnClickListener 로 호출 X
fun buttonClicked(v: View) { }
~~~