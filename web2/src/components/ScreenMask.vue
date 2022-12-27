<template>
  <div
    style="
      position: fixed;
      top: 100px;
      left: 0;
      right: 0;
      z-index: 110;
      background-color: white;
    "
  >
    <div
      style="width: 100%; border: 1px solid red; position: relative"
      :style="{ height: mHeight + 'px' }"
    >
      <vdr
        :x="x"
        :y="y"
        :w="width"
        :h="height"
        v-on:dragging="onDrag"
        v-on:resizing="onResize"
        :parent="true"
        v-if="mHeight && maskActive"
        style="background: #000"
        :style="{ opacity: alpha }"
        :isConflictCheck="true"
        :snap="true"
        :snapTolerance="20"
      >
      </vdr>
    </div>
    <div
      style="
        margin: 10px;
        background: #ddd;
        padding: 10px;
        display: flex;
        justify-content: space-between;
      "
    >
      <span>
        Mask:<a @click="toggleMask()">{{ maskActive ? "ON" : "OFF" }}</a> </span
      ><span>
        Alpha:<select v-model="alpha" @change="changeAlpha()">
          <option
            v-for="i in [
              0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0,
            ].reverse()"
            :key="i"
          >
            {{ i }}
          </option>
        </select>
      </span>
    </div>
  </div>
</template>

<script>
import vdr from "vue-draggable-resizable-gorkys";
import "vue-draggable-resizable-gorkys/dist/VueDraggableResizable.css";
import $ from "jquery";
export default {
  components: { vdr },
  props: ["screenWith", "screenHeight"],
  data: function () {
    return {
      width: 50,
      height: 50,
      mHeight: 0,
      x: 0,
      y: 0,
      timer: 0,
      alpha: 1.0,
    };
  },
  mounted() {
    let p = $(window).width() / this.screenWith;
    this.mHeight = p * this.screenHeight;
    this.width = p * this.screenWith * 0.7;
    this.height = p * this.screenHeight * 0.06;
    this.y = p * this.screenHeight * 0.88;
    this.x = (p * this.screenWith * 0.3) / 2;
    this.alpha = 1.0;
    console.log(
      $(window).width(),
      this.screenWith,
      this.screenHeight,
      this.mHeight
    );
    this.changeAlpha(1);
  },
  methods: {
    toggleMask() {
      let s = this.screenWith / $(window).width();
      $.get("/api/updateMask", {
        sh: this.maskActive ? 0 : 1,
        x: parseInt(s * this.x),
        y: parseInt(s * this.y),
        w: parseInt(s * this.width),
        h: parseInt(s * this.height),
        alpha: this.alpha,
      });
      if (this.maskActive == 1) {
        this.$emit("off", 1);
      }
    },
    changeAlpha(b) {
      let s = this.screenWith / $(window).width();
      $.get("/api/updateMask", {
        sh: b || this.maskActive ? 1 : 0,
        x: parseInt(s * this.x),
        y: parseInt(s * this.y),
        w: parseInt(s * this.width),
        h: parseInt(s * this.height),
        alpha: this.alpha,
      });
    },
    updateMask() {
      clearTimeout(this.timer);
      this.timer = setTimeout(() => {
        let s = this.screenWith / $(window).width();
        $.get("/api/updateMask", {
          sh: 1,
          x: parseInt(s * this.x),
          y: parseInt(s * this.y),
          w: parseInt(s * this.width),
          h: parseInt(s * this.height),
          alpha: this.alpha,
        });
      }, 1000);
    },
    onResize: function (x, y, width, height) {
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
      this.updateMask();
    },
    onDrag: function (x, y) {
      this.x = x;
      this.y = y;
      this.updateMask();
    },
  },
  computed: {
    maskActive() {
      return this.$store.state.playController.maskActive;
    },
  },
};
</script>
