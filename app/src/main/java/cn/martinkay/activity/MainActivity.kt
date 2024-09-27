package cn.martinkay.activity

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cn.martinkay.chiral.ChiralCarbonHelper
import cn.martinkay.chiral.Molecule
import cn.martinkay.chiral.R
import cn.martinkay.chiral.databinding.ActivityMainBinding
import cn.martinkay.util.PubChemStealer

class MainActivity : AppCompatActivity(), View.OnClickListener, Runnable {

    private lateinit var mBinding: ActivityMainBinding
    private val currMol: Molecule? = null
    private var makingMol: AlertDialog? = null
    private var refreshId = 0
    private var mChiralCarbons: java.util.HashSet<Int>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initViews();
    }

    private fun initViews() {
        val moleculeView = mBinding.moleculeView
        moleculeView.setTextColor(Color.BLACK);
        moleculeView.setMolecule(currMol);
        moleculeView.setOnClickListener(this)

        mBinding.reset.setOnClickListener {
            moleculeView.unselectedAllChiral()
            this@MainActivity.onClick(moleculeView)
        }

        mBinding.change.setOnClickListener(this)
        mBinding.next.setOnClickListener(this)

        onClick(mBinding.change)
    }

    override fun onClick(v: View?) {
        when (v) {
            mBinding.moleculeView -> {
                val count: Int = mBinding.moleculeView.getSelectedChiralCount()
                if (count == 0) {
                    mBinding.status.setText("未选择")
                } else {
                    mBinding.status.setText("已选择: $count")
                }
            }
            mBinding.change -> {
                if (makingMol == null) {
                    refreshId++
                    makingMol = AlertDialog.Builder(this).setTitle("请稍候").setMessage("正在加载").show()
                    Thread(this).start()
                }
            }
            mBinding.next -> {
                if (mBinding.moleculeView.getMolecule() == null) {
                    Toast.makeText(this, "请先加载结构式(点\"换一个\")", Toast.LENGTH_SHORT).show()
                    return
                }
                if (mBinding.moleculeView.getSelectedChiralCount() === 0) {
                    Toast.makeText(this, "请选择手性碳原子", Toast.LENGTH_SHORT).show()
                } else {
                    if (mChiralCarbons == null || mChiralCarbons!!.size == 0) {
                        Toast.makeText(this, "未知错误, 请重新加载结构式", Toast.LENGTH_SHORT).show()
                    } else {
                        var pass = true
                        val tmp: HashSet<Int> = HashSet<Int>(mChiralCarbons)
                        for (i in mBinding.moleculeView.getSelectedChiral()) {
                            if (tmp.contains(i)) {
                                tmp.remove(i)
                            } else {
                                pass = false
                                break
                            }
                        }
                        if (tmp.size > 0) pass = false
                        if (pass) {
                            Toast.makeText(this, "验证成功", Toast.LENGTH_SHORT).show()
                            mBinding.moleculeView.setEnabled(false)
                            mBinding.change.setVisibility(View.GONE)
                            mBinding.reset.setVisibility(View.GONE)
                            mBinding.next.setText("验证已完成")
                            mBinding.next.setEnabled(false)
//                            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                        } else {
                            Toast.makeText(this, "选择错误, 请重试", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    override fun run() {
        val curr = refreshId
        var mol: Molecule
        var cc: java.util.HashSet<Int>? = null
        do {
            mol = PubChemStealer.nextRandomMolecule()
            if (mol != null) cc = ChiralCarbonHelper.getMoleculeChiralCarbons(mol)
        } while (mol != null && curr == refreshId && cc!!.size < 3)
        val molecule = mol
        if (makingMol != null) {
            makingMol!!.dismiss()
            makingMol = null
        } else {
            return
        }
        if (curr != refreshId) return
        if (molecule != null) {
            val finalCc = cc
            runOnUiThread {
                mChiralCarbons = finalCc
                mBinding.moleculeView.setMolecule(molecule)
                onClick(mBinding.moleculeView)
            }
        } else {
            runOnUiThread {
                Toast.makeText(this@MainActivity, "加载失败", Toast.LENGTH_SHORT).show()
            }
        }
    }


}