package com.gjuhasz.mforecast.shared.lib

import java.time.{LocalDate, Period}

import com.gjuhasz.mforecast.shared.lib.Schedule.Syntax.OUT
import com.gjuhasz.mforecast.shared.lib.Utils._

import scala.language.higherKinds

trait FloatingSchedule[T] {
  def startAt(start: LocalDate): FixSchedule[T] = FixSchedule(start, this)
  def rollout(start: LocalDate): List[(LocalDate, T)]
  def applyToItem(f: T => T): FloatingSchedule[T]
}

case class NoFloatingSchedule[T](item: T) extends FloatingSchedule[T] {
  override def rollout(start: LocalDate): List[(LocalDate, T)] = List((start, item))
  override def applyToItem(f: T => T): FloatingSchedule[T] = NoFloatingSchedule(f(item))
}

case class FloatingScheduleImpl[T](innerSchedule: FloatingSchedule[T],
  frequency: Period, duration: Period, shift: Period, tranform: T => T) extends FloatingSchedule[T] {

  override def rollout(start: LocalDate): List[(LocalDate, T)] = {
    val startDates =
      Stream.from(0)
        .map(i => start + (frequency * i))
        .takeWhile(_.isBefore(start + duration))
        .toList

    def roll(schedule: FloatingSchedule[T], dates: List[LocalDate]): List[(LocalDate, T)] = dates match {
      case Nil => Nil
      case head :: tail =>
        val nextSch = schedule.applyToItem(tranform)
        schedule.rollout(head) ::: roll(nextSch, tail)
    }

    roll(innerSchedule, startDates)
  }

  override def applyToItem(f: T => T): FloatingSchedule[T] = this.copy(innerSchedule = innerSchedule.applyToItem(f))
}

case class FixSchedule[T](start: LocalDate, schedule: FloatingSchedule[T]) {
  def ROLL(out: OUT): List[(LocalDate, T)] = schedule.rollout(start)
}

object Schedule {
  object Syntax {
    sealed trait OUT
    sealed trait IT
    val OUT = new OUT {}
    val IT = new IT {}
    object Hidden {
      type None[T] = None.type
    }

    import Hidden._

    case class Builder[Freq[_] <: Option[_], Dur[_] <: Option[_], Shift[_] <: Option[_], Trans[_] <: Option[_], Start[_] <: Option[_], T](
      frequency: Freq[Period], duration: Dur[Period], shift: Shift[Period], transform: Trans[T => T], start: Start[LocalDate], sched: FloatingSchedule[T])

    implicit class ScheduleToBuilder[S, T](self: S)(implicit ev: S => FloatingSchedule[T]) {
      def ~ : ScheduleToBuilder[S, T] = this
      def REPEAT(it: IT): Builder[None, None, None, None, None, T] =
        Builder[None, None, None, None, None, T](None, None, None, None, None, self)
    }

    def REPEAT[T](self: T): Builder[None, None, None, None, None, T] =
      Builder[None, None, None, None, None, T](None, None, None, None, None, NoFloatingSchedule(self))

    implicit class FreqBuilder[Freq[_] <: Option[_], Dur[_] <: Option[_], Shift[_] <: Option[_], Trans[_] <: Option[_], Start[_] <: Option[_], T](
      self: Builder[Freq, Dur, Shift, Trans, Start, T])(implicit ev: Freq[_] <:< None[_]) {
      def EVERY(frequency: Period): Builder[Some, Dur, Shift, Trans, Start, T] =
        self.copy(frequency = Some(frequency))
    }

    implicit class DurBuilder[Freq[_] <: Option[_], Dur[_] <: Option[_], Shift[_] <: Option[_], Trans[_] <: Option[_], Start[_] <: Option[_], T](
      self: Builder[Freq, Dur, Shift, Trans, Start, T])(implicit ev: Dur[_] <:< None[_]) {
      def FOR(duration: Period): Builder[Freq, Some, Shift, Trans, Start, T] =
        self.copy(duration = Some(duration))
    }

    implicit class ShiftBuilder[Freq[_] <: Option[_], Dur[_] <: Option[_], Shift[_] <: Option[_], Trans[_] <: Option[_], Start[_] <: Option[_], T](
      self: Builder[Freq, Dur, Shift, Trans, Start, T])(implicit ev: Shift[_] <:< None[_]) {
      def SHIFTED(shift: Period): Builder[Freq, Dur, Some, Trans, Start, T] =
        self.copy(shift = Some(shift))
    }

    implicit class TransBuilder[Freq[_] <: Option[_], Dur[_] <: Option[_], Shift[_] <: Option[_], Trans[_] <: Option[_], Start[_] <: Option[_], T](
      self: Builder[Freq, Dur, Shift, Trans, Start, T])(implicit ev: Trans[_] <:< None[_]) {
      def TRANSFORMED(transform: T => T): Builder[Freq, Dur, Shift, Some, Start, T] =
        self.copy(transform = Some(transform))
    }

    implicit class StartBuilder[Freq[_] <: Option[_], Dur[_] <: Option[_], Shift[_] <: Option[_], Trans[_] <: Option[_], Start[_] <: Option[_], T](
      self: Builder[Freq, Dur, Shift, Trans, Start, T])(implicit ev: Start[_] <:< None[_]) {
      def START_ON(start: LocalDate): Builder[Freq, Dur, Shift, Trans, Some, T] =
        self.copy(start = Some(start))
    }

    implicit def CompleteFloatingSchedule[Freq[_] <: Option[_], Dur[_] <: Option[_], Shift[_] <: Option[_], Trans[_] <: Option[_], Start[_] <: Option[_], T](
      self: Builder[Freq, Dur, Shift, Trans, Start, T])(
      implicit freqEv: Freq[Period] <:< Some[Period], durEv: Dur[Period] <:< Some[Period], shiftEv: Shift[Period] <:< Option[Period], transEv: Trans[T => T] <:< Option[T => T]): FloatingSchedule[T] = {

      val freq = freqEv(self.frequency).safeGet
      val dur = durEv(self.duration).safeGet
      val shiftOpt: Option[Period] = self.shift
      val shift: Period = shiftOpt.getOrElse(0.day)

      val transOpt: Option[T => T] = self.transform
      val trans: T => T = transOpt.getOrElse(identity[T])

      FloatingScheduleImpl[T](self.sched, freq, dur, shift, trans)
    }

    implicit def CompleteFixSchedule[Freq[_] <: Option[_], Dur[_] <: Option[_], Shift[_] <: Option[_], Trans[_] <: Option[_], Start[_] <: Option[_], T](
      self: Builder[Freq, Dur, Shift, Trans, Start, T])(
      implicit freqEv: Freq[Period] <:< Some[Period], durEv: Dur[Period] <:< Some[Period], startEv: Start[LocalDate] <:< Some[LocalDate], shiftEv: Shift[Period] <:< Option[Period], transEv: Trans[T => T] <:< Option[T => T]): FixSchedule[T] = {

      val start = startEv(self.start).safeGet
      FixSchedule(start, CompleteFloatingSchedule(self))
    }

    type Schedule[T] = List[(LocalDate, T)]
  }
}