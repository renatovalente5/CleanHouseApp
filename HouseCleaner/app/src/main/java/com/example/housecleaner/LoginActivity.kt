package com.example.housecleaner

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isVisible
import com.example.housecleaner.model.User
import com.example.housecleaner.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var progressDialog: ProgressDialog
    var isLogged: Boolean = false
    var rememberLogin: Boolean = false
    var email: String = ""
    var password: String = ""
    var login: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        binding.tilUsername.isVisible = false

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Loading")
        progressDialog.setMessage("Logging In...")
        progressDialog.setCanceledOnTouchOutside(false)

        loginPreferences()

        binding.rememberCheckBox.isChecked = rememberLogin
        if(rememberLogin) {
            binding.etEmail.setText(email)
            binding.etPassword.setText(password)
        }

        firebaseAuth = FirebaseAuth.getInstance()
        checkUser()

        binding.loginBtn.setOnClickListener {
            when {
                TextUtils.isEmpty(binding.etEmail.text.toString().trim()) -> {
                    Toast.makeText(
                        this@LoginActivity,
                        "Please enter email!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                !Patterns.EMAIL_ADDRESS.matcher(binding.etEmail.text.toString().trim()).matches() -> {
                    Toast.makeText(
                        this@LoginActivity,
                        "Invalid email format!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                TextUtils.isEmpty(binding.etPassword.text.toString()) -> {
                    Toast.makeText(
                        this@LoginActivity,
                        "Please enter password!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                binding.etPassword.text.toString().length < 6 -> {
                    Toast.makeText(
                        this@LoginActivity,
                        "Password must contain at least 6 characters!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                login -> {
                    login(binding.etEmail.text.toString(), binding.etPassword.text.toString(), binding.rememberCheckBox.isChecked)
                }
                TextUtils.isEmpty(binding.etName.text.toString().trim()) -> {
                    Toast.makeText(
                        this@LoginActivity,
                        "Please enter username!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                !login -> {
                    val name = binding.etName.text.toString().trim()
                    val mail = binding.etEmail.text.toString().trim()
                    val pass = binding.etPassword.text.toString()

                    database = FirebaseDatabase.getInstance("https://housecleanaveiro-default-rtdb.europe-west1.firebasedatabase.app/")
                    progressDialog.show()
                    FirebaseAuth.getInstance().createUserWithEmailAndPassword(mail, pass)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val firebaseUser: FirebaseUser = task.result!!.user!!
                                val user = User(firebaseUser.uid, name, mail)
                                database.getReference("Cleaners").child(firebaseUser.uid).setValue(user).addOnSuccessListener {
                                    Toast.makeText(
                                        this@LoginActivity,
                                        "User registered sucessfully!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    saveLoginPreferences(
                                        true,
                                        binding.rememberCheckBox.isChecked,
                                        binding.etEmail.text.toString(),
                                        binding.etPassword.text.toString()
                                    )
                                    progressDialog.dismiss()
                                    val intent =
                                        Intent(this@LoginActivity, MainActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                    finish()
                                }.addOnFailureListener{
                                    progressDialog.dismiss()
                                    Toast.makeText(
                                        this@LoginActivity,
                                        "Database error!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            } else {
                                progressDialog.dismiss()
                                Toast.makeText(
                                    this@LoginActivity,
                                    "User registration failed!",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                }
            }
        }
        binding.registerText.setOnClickListener {
            login = !login
            if(login){
                binding.tilUsername.isVisible = false
                binding.logintext.setText("LOGIN")
                binding.loginBtn.setText("LOGIN")
                binding.registerText.setText("Register")
                binding.loginOrRegirsterText.setText("Don't have an accoun?")
            } else {
                binding.tilUsername.isVisible = true
                binding.logintext.setText("REGISTER")
                binding.loginBtn.setText("REGISTER")
                binding.registerText.setText("Login")
                binding.loginOrRegirsterText.setText("Already have an accoun?")
            }
        }
    }

    private fun checkUser() {
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser != null){
            val intent =
                Intent(this@LoginActivity, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun login(etmail: String, etpass: String, remember: Boolean) {
        progressDialog.show()
        val mail: String = etmail.trim{ it <= ' '}
        val pass: String = etpass.trim{ it <= ' '}
        FirebaseAuth.getInstance().signInWithEmailAndPassword(mail, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    saveLoginPreferences(true, remember, mail, pass)
                    progressDialog.dismiss()
                    val intent =
                        Intent(this@LoginActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@LoginActivity,
                        "Login Failed!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun loginPreferences() {
        val loginPreferences: SharedPreferences = getSharedPreferences(
            "loginPrefsCleaner",
            Context.MODE_PRIVATE
        )
        val editor: SharedPreferences.Editor = loginPreferences.edit()
        if(!loginPreferences.contains("INITIALIZED")){
            editor.apply {
                editor.putBoolean("INITIALIZED", true)
                editor.putBoolean("IS_LOGGED", isLogged)
                editor.putBoolean("REMEMBER_LOGIN", rememberLogin)
                editor.putString("EMAIL", email)
                editor.putString("PASSWORD", password)
            }.apply()
            Toast.makeText(this, "Login Prefs Created!", Toast.LENGTH_SHORT).show()
        } else {
            isLogged = loginPreferences.getBoolean("IS_LOGGED", false)
            rememberLogin = loginPreferences.getBoolean("REMEMBER_LOGIN", false)
            email = loginPreferences.getString("EMAIL", "").toString()
            password = loginPreferences.getString("PASSWORD", "").toString()
        }
    }

    private fun saveLoginPreferences(logged: Boolean, remember: Boolean, em: String, pwd: String) {
        val loginPreferences: SharedPreferences = getSharedPreferences(
            "loginPrefs",
            Context.MODE_PRIVATE
        )
        val editor: SharedPreferences.Editor = loginPreferences.edit()
        editor.apply {
            editor.putBoolean("IS_LOGGED", logged)
            editor.putBoolean("REMEMBER_LOGIN", remember)
            if (remember) {
                editor.putString("EMAIL", em)
                editor.putString("PASSWORD", pwd)
            } else {
                editor.putString("EMAIL", "")
                editor.putString("PASSWORD", "")
            }
        }.apply()
    }
}